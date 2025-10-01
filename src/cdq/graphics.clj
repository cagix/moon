(ns cdq.graphics
  (:require [cdq.graphics.cursors :as cursors]
            [cdq.graphics.font :as font]
            [cdq.graphics.shape-drawer :as shape-drawer]
            [cdq.graphics.shape-drawer-texture :as shape-drawer-texture]
            [cdq.graphics.sprite-batch :as sprite-batch]
            [clojure.graphics.orthographic-camera :as camera]
            [clojure.graphics.viewport :as viewport]
            [com.badlogic.gdx.files :as files]
            [com.badlogic.gdx.files.utils :as files-utils]
            [com.badlogic.gdx.graphics :as graphics]
            [com.badlogic.gdx.graphics.orthographic-camera :as orthographic-camera]
            [com.badlogic.gdx.graphics.pixmap :as pixmap]
            [com.badlogic.gdx.graphics.texture :as texture]
            [com.badlogic.gdx.graphics.color :as color]
            [com.badlogic.gdx.graphics.colors :as colors]
            [com.badlogic.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]))

(defprotocol PGraphics
  (clear! [_ [r g b a]])
  (draw-tiled-map! [_ tiled-map color-setter])
  (set-cursor! [_ cursor-key])
  (delta-time [_])
  (frames-per-second [_])
  (world-viewport-width [_])
  (world-viewport-height [_])
  (camera-position [_])
  (visible-tiles [_])
  (camera-frustum [_])
  (camera-zoom [_])
  (change-zoom! [_ amount])
  (set-camera-position! [_ position])
  (texture-region [_ image])
  (update-viewports! [_ width height])
  (unproject-ui [_ position])
  (unproject-world [_ position]))

(defrecord RGraphics []
  PGraphics
  (clear! [{:keys [graphics/core]} [r g b a]]
    (graphics/clear! core r g b a))

  (draw-tiled-map!
    [{:keys [graphics/tiled-map-renderer
             graphics/world-viewport]}
     tiled-map
     color-setter]
    (tm-renderer/draw! tiled-map-renderer
                       world-viewport
                       tiled-map
                       color-setter))

  (set-cursor!
    [{:keys [graphics/cursors
             graphics/core]}
     cursor-key]
    (assert (contains? cursors cursor-key))
    (graphics/set-cursor! core (get cursors cursor-key)))

  (delta-time
    [{:keys [graphics/core]}]
    (graphics/delta-time core))

  (frames-per-second
    [{:keys [graphics/core]}]
    (graphics/frames-per-second core))

  (world-viewport-width  [{:keys [graphics/world-viewport]}] (:viewport/width  world-viewport))
  (world-viewport-height [{:keys [graphics/world-viewport]}] (:viewport/height world-viewport))

  (camera-position [{:keys [graphics/world-viewport]}] (:camera/position     (:viewport/camera world-viewport)))
  (visible-tiles   [{:keys [graphics/world-viewport]}] (camera/visible-tiles (:viewport/camera world-viewport)))
  (camera-frustum  [{:keys [graphics/world-viewport]}] (camera/frustum       (:viewport/camera world-viewport)))
  (camera-zoom     [{:keys [graphics/world-viewport]}] (:camera/zoom         (:viewport/camera world-viewport)))

  (change-zoom! [{:keys [graphics/world-viewport]} amount]
    (camera/inc-zoom! (:viewport/camera world-viewport) amount))

  (set-camera-position! [{:keys [graphics/world-viewport]} position]
    (camera/set-position! (:viewport/camera world-viewport) position))

  (unproject-ui [{:keys [graphics/ui-viewport]
                  :as graphics}
                 position]
    (assoc graphics :graphics/ui-mouse-position (viewport/unproject ui-viewport position)))

  (unproject-world [{:keys [graphics/world-viewport]
                     :as graphics}
                    position]
    (assoc graphics :graphics/world-mouse-position (viewport/unproject world-viewport position)))

  (update-viewports! [{:keys [graphics/ui-viewport
                              graphics/world-viewport]} width height]
    (viewport/update! ui-viewport    width height {:center? true})
    (viewport/update! world-viewport width height {:center? false}))

  (texture-region [{:keys [graphics/textures]}
                   {:keys [image/file image/bounds]}]
    (assert file)
    (assert (contains? textures file))
    (let [texture (get textures file)]
      (if bounds
        (texture/region texture bounds)
        (texture/region texture)))))

(defn- create-textures
  [{:keys [graphics/core]
    :as graphics} textures-to-load]
  (assoc graphics :graphics/textures
         (into {} (for [[path file-handle] textures-to-load]
                    [path (graphics/texture core file-handle)]))))

(defn- add-unit-scales [graphics world-unit-scale]
  (assoc graphics
         :graphics/unit-scale (atom 1)
         :graphics/world-unit-scale world-unit-scale))

(defn- tiled-map-renderer [{:keys [graphics/batch
                                   graphics/world-unit-scale]
                            :as graphics}]
  (assoc graphics :graphics/tiled-map-renderer (tm-renderer/create world-unit-scale batch)))

(defn- create-ui-viewport
  [{:keys [graphics/core]
    :as graphics} ui-viewport]
  (assoc graphics :graphics/ui-viewport (graphics/fit-viewport core
                                                               (:width  ui-viewport)
                                                               (:height ui-viewport)
                                                               (orthographic-camera/create))))

(defn- create-world-viewport
  [{:keys [graphics/core
           graphics/world-unit-scale]
    :as graphics}
   world-viewport]
  (assoc graphics :graphics/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                                 world-height (* (:height world-viewport) world-unit-scale)]
                                             (graphics/fit-viewport core
                                                                    world-width
                                                                    world-height
                                                                    (orthographic-camera/create
                                                                     :y-down? false
                                                                     :world-width world-width
                                                                     :world-height world-height)))))

(defn- create*
  [{:keys [textures-to-load
           world-unit-scale
           ui-viewport
           default-font
           cursors
           world-viewport]}
   graphics]
  (-> (map->RGraphics {})
      (assoc :graphics/core graphics)
      (cursors/create cursors)
      (font/create default-font)
      sprite-batch/create
      shape-drawer-texture/create
      shape-drawer/create
      (create-textures textures-to-load)
      (add-unit-scales world-unit-scale)
      tiled-map-renderer
      (create-ui-viewport ui-viewport)
      (create-world-viewport world-viewport)))

(defn- handle-files
  [files {:keys [colors
                 cursors
                 default-font
                 tile-size
                 texture-folder
                 ui-viewport
                 world-viewport]}]
  {:ui-viewport ui-viewport
   :default-font {:file-handle (files/internal files (:path default-font))
                  :params (:params default-font)}
   :colors colors
   :cursors (update-vals (:data cursors)
                         (fn [[short-path hotspot]]
                           [(files/internal files (format (:path-format cursors) short-path))
                            hotspot]))
   :world-unit-scale (float (/ tile-size))
   :world-viewport world-viewport
   :textures-to-load (files-utils/search files texture-folder)})

(defn create! [files graphics config]
  (doseq [[name rgba] (:colors config)]
    (colors/put! name (color/create rgba)))
  (-> (handle-files files config)
      (create* graphics)))
