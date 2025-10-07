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
            [clojure.gdx.graphics.orthographic-camera :as camera]
            [clojure.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.shape-drawer :as sd]
            [clojure.gdx.viewport :as viewport]))

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
    (batch/set-color! batch color/white)
    (batch/set-projection-matrix! batch (camera/combined (viewport/camera world-viewport)))
    (batch/begin! batch)
    (sd/with-line-width shape-drawer world-unit-scale
      (reset! unit-scale world-unit-scale)
      (f)
      (reset! unit-scale 1))
    (batch/end! batch)))

(defn create!
  [{:keys [colors
           textures-to-load
           world-unit-scale
           ui-viewport
           default-font
           cursors
           world-viewport]}
   graphics]
  (doseq [[name rgba] colors]
    (colors/put! name (color/create rgba)))
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
