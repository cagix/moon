(ns cdq.ctx.create-graphics
  (:require [cdq.graphics]
            [cdq.graphics.cursors :as cursors]
            [cdq.graphics.font :as font]
            [cdq.graphics.shape-drawer :as shape-drawer]
            [cdq.graphics.shape-drawer-texture :as shape-drawer-texture]
            [cdq.graphics.sprite-batch :as sprite-batch]
            [cdq.graphics.textures :as textures]
            [cdq.graphics.tiled-map :as tiled-map]
            [cdq.graphics.ui-viewport :as ui-viewport]
            [cdq.graphics.unit-scale :as unit-scale]
            [cdq.graphics.world-viewport :as world-viewport]
            [clojure.graphics.orthographic-camera :as camera]
            [clojure.graphics.viewport :as viewport]
            [gdl.disposable :as disposable]
            [gdl.files :as files]
            [gdl.files.utils :as files-utils]
            [com.badlogic.gdx.graphics :as graphics]
            [com.badlogic.gdx.graphics.pixmap :as pixmap]
            [com.badlogic.gdx.graphics.color :as color]
            [com.badlogic.gdx.graphics.colors :as colors]
            [com.badlogic.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]))

(defrecord RGraphics []
  disposable/Disposable
  (dispose! [{:keys [graphics/batch
                     graphics/cursors
                     graphics/default-font
                     graphics/shape-drawer-texture
                     graphics/textures]}]
    (disposable/dispose! batch)
    (run! disposable/dispose! (vals cursors))
    (disposable/dispose! default-font)
    (disposable/dispose! shape-drawer-texture)
    (run! disposable/dispose! (vals textures)))
  cdq.graphics/PGraphics
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
    (viewport/update! world-viewport width height {:center? false})))

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
      (textures/create textures-to-load)
      (unit-scale/create world-unit-scale)
      tiled-map/renderer
      (ui-viewport/create ui-viewport)
      (world-viewport/create world-viewport)))

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

(defn do!
  [{:keys [ctx/files
           ctx/graphics]
    :as ctx}
   params]
  (doseq [[name rgba] (:colors params)]
    (colors/put! name (color/create rgba)))
  (assoc ctx :ctx/graphics (-> (handle-files files params)
                               (create* graphics))))
