(ns cdq.graphics.impl
  (:require [cdq.graphics]
            [cdq.graphics.camera]
            [cdq.graphics.draw.arc]
            [cdq.graphics.draw.circle]
            [cdq.graphics.draw.ellipse]
            [cdq.graphics.draw.filled-circle]
            [cdq.graphics.draw.filled-ellipse]
            [cdq.graphics.draw.filled-rectangle]
            [cdq.graphics.draw.grid]
            [cdq.graphics.draw.line]
            [cdq.graphics.draw.rectangle]
            [cdq.graphics.draw.sector]
            [cdq.graphics.draw.text]
            [cdq.graphics.draw.texture-region]
            [cdq.graphics.draw.with-line-width]
            [cdq.graphics.draws :as draws]
            [cdq.graphics.textures]
            [cdq.graphics.tiled-map-renderer]
            [cdq.graphics.ui-viewport]
            [cdq.graphics.world-viewport]
            [clojure.gdx.graphics.g2d.freetype.generator :as generator]
            [clojure.gdx.graphics.g2d.freetype.parameter :as parameter]
            [clojure.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.colors :as colors]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [clojure.gdx.graphics.texture :as texture]
            [clojure.gdx.graphics.texture.filter :as texture-filter]
            [clojure.gdx.graphics.orthographic-camera :as orthographic-camera]
            [clojure.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [clojure.disposable :as disposable :refer [dispose!]]
            [clojure.files :as files]
            [clojure.files.utils :as files-utils]
            [clojure.graphics :as graphics]
            [clojure.graphics.batch :as batch]
            [clojure.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [clojure.graphics.color]
            [clojure.graphics.orthographic-camera :as camera]
            [clojure.graphics.shape-drawer :as sd]
            [clojure.graphics.viewport :as viewport]
            [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport]))

(defn- create-world-viewport
  [{:keys [graphics/world-unit-scale]
    :as graphics}
   world-viewport]
  (assoc graphics :graphics/world-viewport (let [world-width  (* (:width  world-viewport) world-unit-scale)
                                                 world-height (* (:height world-viewport) world-unit-scale)]
                                             (fit-viewport/create world-width
                                                                  world-height
                                                                  (orthographic-camera/create
                                                                   :y-down? false
                                                                   :world-width world-width
                                                                   :world-height world-height)))))

(defn- create-unit-scales [graphics world-unit-scale]
  (assoc graphics
         :graphics/unit-scale (atom 1)
         :graphics/world-unit-scale world-unit-scale))

(defn- create-ui-viewport
  [{:keys [graphics/core]
    :as graphics} ui-viewport]
  (assoc graphics :graphics/ui-viewport (fit-viewport/create (:width  ui-viewport)
                                                             (:height ui-viewport)
                                                             (orthographic-camera/create))))

(defn- create-tm-renderer
  [{:keys [graphics/batch
           graphics/world-unit-scale]
    :as graphics}]
  (assoc graphics :graphics/tiled-map-renderer (tm-renderer/create world-unit-scale batch)))

(defn- create-textures
  [graphics textures-to-load]
  (extend-type (class graphics)
    cdq.graphics.textures/Textures
    (texture-region [{:keys [graphics/textures]}
                     {:keys [image/file image/bounds]}]
      (assert file)
      (assert (contains? textures file))
      (let [texture (get textures file)]
        (if bounds
          (texture/region texture bounds)
          (texture/region texture)))))
  (assoc graphics :graphics/textures
         (into {} (for [[path file-handle] textures-to-load]
                    [path (texture/create file-handle)]))))

(defn- create-sprite-batch
  [graphics]
  (assoc graphics :graphics/batch (sprite-batch/create)))

(defn- create-shape-drawer-texture
  [graphics]
  (assoc graphics :graphics/shape-drawer-texture (let [pixmap (doto (pixmap/create 1 1 :pixmap.format/RGBA8888)
                                                                (pixmap/set-color! clojure.graphics.color/white)
                                                                (pixmap/draw-pixel! 0 0))
                                                       texture (pixmap/texture pixmap)]
                                                   (dispose! pixmap)
                                                   texture)))

(defn- create-shape-drawer
  [{:keys [graphics/batch
           graphics/shape-drawer-texture]
    :as graphics}]
  (assoc graphics :graphics/shape-drawer (sd/create batch (texture/region shape-drawer-texture 1 0 1 1))))

(defn- create-font [graphics default-font]
  (assoc graphics :graphics/default-font
         (let [file-handle (:file-handle default-font)
               {:keys [size
                       quality-scaling
                       enable-markup?
                       use-integer-positions?
                       min-filter
                       mag-filter]} (:params default-font)]
           (let [generator (generator/create file-handle)
                 font (generator/generate-font generator
                                               (parameter/create {:size (* size quality-scaling)
                                                                  :min-filter (texture-filter/k->value min-filter)
                                                                  :mag-filter (texture-filter/k->value mag-filter)}))]
             (bitmap-font/set-scale!                 font (/ quality-scaling))
             (bitmap-font/enable-markup!             font enable-markup?)
             (bitmap-font/set-use-integer-positions! font use-integer-positions?)
             font))))

(defn- create-cursors
  [{:keys [graphics/core]
    :as graphics}
   cursors]
  (assoc graphics :graphics/cursors (update-vals cursors
                                                 (fn [[file-handle [hotspot-x hotspot-y]]]
                                                   (let [pixmap (pixmap/create file-handle)
                                                         cursor (graphics/cursor core pixmap hotspot-x hotspot-y)]
                                                     (dispose! pixmap)
                                                     cursor)))))

(def ^:private draw-fns
  {:draw/with-line-width  cdq.graphics.draw.with-line-width/do!
   :draw/grid             cdq.graphics.draw.grid/do!
   :draw/texture-region   cdq.graphics.draw.texture-region/do!
   :draw/text             cdq.graphics.draw.text/do!
   :draw/ellipse          cdq.graphics.draw.ellipse/do!
   :draw/filled-ellipse   cdq.graphics.draw.filled-ellipse/do!
   :draw/circle           cdq.graphics.draw.circle/do!
   :draw/filled-circle    cdq.graphics.draw.filled-circle/do!
   :draw/rectangle        cdq.graphics.draw.rectangle/do!
   :draw/filled-rectangle cdq.graphics.draw.filled-rectangle/do!
   :draw/arc              cdq.graphics.draw.arc/do!
   :draw/sector           cdq.graphics.draw.sector/do!
   :draw/line             cdq.graphics.draw.line/do!})

(defrecord Graphics []
  cdq.graphics.camera/Camera
  (position [{:keys [graphics/world-viewport]}]
    (:camera/position (viewport/camera world-viewport)))

  (visible-tiles [{:keys [graphics/world-viewport]}]
    (camera/visible-tiles (viewport/camera world-viewport)))

  (frustum [{:keys [graphics/world-viewport]}]
    (camera/frustum (viewport/camera world-viewport)))

  (zoom [{:keys [graphics/world-viewport]}]
    (:camera/zoom (viewport/camera world-viewport)))

  (change-zoom! [{:keys [graphics/world-viewport]} amount]
    (camera/inc-zoom! (viewport/camera world-viewport) amount))

  (set-position! [{:keys [graphics/world-viewport]} position]
    (camera/set-position! (viewport/camera world-viewport) position))

  disposable/Disposable
  (dispose! [{:keys [graphics/batch
                     graphics/cursors
                     graphics/default-font
                     graphics/shape-drawer-texture
                     graphics/textures]}]
    (dispose! batch)
    (run! dispose! (vals cursors))
    (dispose! default-font)
    (dispose! shape-drawer-texture)
    (run! dispose! (vals textures)))

  draws/Draws
  (handle! [graphics draws]
    (doseq [{k 0 :as component} draws
            :when component]
      (apply (draw-fns k) graphics (rest component))))

  cdq.graphics.tiled-map-renderer/TiledMapRenderer
  (draw!
    [{:keys [graphics/tiled-map-renderer
             graphics/world-viewport]}
     tiled-map
     color-setter]
    (tm-renderer/draw! tiled-map-renderer
                       world-viewport
                       tiled-map
                       color-setter))

  cdq.graphics.ui-viewport/UIViewport
  (unproject [{:keys [graphics/ui-viewport]} position]
    (viewport/unproject ui-viewport position))

  (update! [{:keys [graphics/ui-viewport]} width height]
    (viewport/update! ui-viewport width height {:center? true}))

  cdq.graphics/Graphics
  (clear! [{:keys [graphics/core]} color]
    (graphics/clear! core color))

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
    (graphics/frames-per-second core)))

(extend-type Graphics
  cdq.graphics.world-viewport/WorldViewport
  (width [{:keys [graphics/world-viewport]}]
    (viewport/world-width world-viewport))

  (height [{:keys [graphics/world-viewport]}]
    (viewport/world-height world-viewport))

  (unproject [{:keys [graphics/world-viewport]} position]
    (viewport/unproject world-viewport position))

  (update! [{:keys [graphics/world-viewport]} width height]
    (viewport/update! world-viewport width height {:center? false}))

  (draw! [{:keys [graphics/batch
                  graphics/shape-drawer
                  graphics/unit-scale
                  graphics/world-unit-scale
                  graphics/world-viewport]}
          f]
    ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
    ; -> also Widgets, etc. ? check.
    (batch/set-color! batch clojure.graphics.color/white)
    (batch/set-projection-matrix! batch (:camera/combined (viewport/camera world-viewport)))
    (batch/begin! batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (reset! unit-scale world-unit-scale)
      (f)
      (reset! unit-scale 1))
    (batch/end! batch)))

(defn- create*
  [{:keys [textures-to-load
           world-unit-scale
           ui-viewport
           default-font
           cursors
           world-viewport]}
   graphics]
  (-> (map->Graphics {})
      (assoc :graphics/core graphics)
      (create-cursors cursors)
      (create-font default-font)
      create-sprite-batch
      create-shape-drawer-texture
      create-shape-drawer
      (create-textures textures-to-load)
      (create-unit-scales world-unit-scale)
      create-tm-renderer
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

(defn create [graphics files params]
  (doseq [[name rgba] (:colors params)]
    (colors/put! name (color/create rgba)))
  (-> (handle-files files params)
      (create* graphics)))
