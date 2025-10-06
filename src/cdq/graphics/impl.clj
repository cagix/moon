(ns cdq.graphics.impl
  (:require [cdq.graphics]
            [cdq.graphics.create.batch]
            [cdq.graphics.create.cursors]
            [cdq.graphics.create.default-font]
            [cdq.graphics.create.shape-drawer]
            [cdq.graphics.create.shape-drawer-texture]
            [cdq.graphics.create.textures]
            [cdq.graphics.create.tiled-map-renderer]
            [cdq.graphics.create.ui-viewport]
            [cdq.graphics.create.unit-scales]
            [cdq.graphics.create.world-viewport]
            [cdq.graphics.tiled-map-renderer]
            [cdq.graphics.ui-viewport]
            [cdq.graphics.world-viewport]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.colors :as colors]
            [clojure.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [clojure.files :as files]
            [clojure.files.utils :as files-utils]
            [clojure.graphics :as graphics]
            [clojure.graphics.batch :as batch]
            [clojure.graphics.color]
            [clojure.graphics.shape-drawer :as sd]
            [clojure.graphics.viewport :as viewport]))

(defrecord Graphics []
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
      (cdq.graphics.create.cursors/create cursors)
      (cdq.graphics.create.default-font/create default-font)
      cdq.graphics.create.batch/create
      cdq.graphics.create.shape-drawer-texture/create
      cdq.graphics.create.shape-drawer/create
      (cdq.graphics.create.textures/create textures-to-load)
      (cdq.graphics.create.unit-scales/create world-unit-scale)
      cdq.graphics.create.tiled-map-renderer/create
      (cdq.graphics.create.ui-viewport/create ui-viewport)
      (cdq.graphics.create.world-viewport/create world-viewport)))

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

(defn create
  [{:keys [clojure.gdx/graphics
           clojure.gdx/files]}
   params]
  (doseq [[name rgba] (:colors params)]
    (colors/put! name (color/create rgba)))
  (-> (handle-files files params)
      (create* graphics)))
