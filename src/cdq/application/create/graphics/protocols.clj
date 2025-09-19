(ns cdq.application.create.graphics.protocols
  (:require [cdq.graphics]
            [com.badlogic.gdx.graphics.orthographic-camera :as camera]
            [com.badlogic.gdx.graphics.texture :as texture]
            [com.badlogic.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [com.badlogic.gdx.utils.disposable :as disposable]
            [com.badlogic.gdx.utils.viewport :as viewport]
            [gdl.graphics :as graphics]
            [gdl.graphics.color :as color]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.graphics.g2d.batch :as batch]))

(defn do! [graphics]
  (extend-type (class graphics)
    cdq.graphics/Graphics
    (clear! [{:keys [graphics/core]} [r g b a]]
      (graphics/clear! core r g b a))

    (dispose!
      [{:keys [graphics/batch
               graphics/cursors
               graphics/default-font
               graphics/shape-drawer-texture
               graphics/textures]}]
      (disposable/dispose! batch)
      (run! disposable/dispose! (vals cursors))
      (disposable/dispose! default-font)
      (disposable/dispose! shape-drawer-texture)
      (run! disposable/dispose! (vals textures)))

    (draw-on-world-viewport!
      [{:keys [graphics/batch
               graphics/shape-drawer
               graphics/unit-scale
               graphics/world-unit-scale
               graphics/world-viewport]}
       f]
      ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
      ; -> also Widgets, etc. ? check.
      (batch/set-color! batch color/white)
      (batch/set-projection-matrix! batch (:camera/combined (:viewport/camera world-viewport)))
      (batch/begin! batch)
      (sd/with-line-width shape-drawer world-unit-scale
        (fn []
          (reset! unit-scale world-unit-scale)
          (f)
          (reset! unit-scale 1)))
      (batch/end! batch))

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

    (unproject-ui    [{:keys [graphics/ui-viewport]}    position] (viewport/unproject-clamp ui-viewport    position))
    (unproject-world [{:keys [graphics/world-viewport]} position] (viewport/unproject-clamp world-viewport position))

    (update-viewports! [{:keys [graphics/ui-viewport
                                graphics/world-viewport]} width height]
      (viewport/update! ui-viewport    width height :center? true)
      (viewport/update! world-viewport width height :center? false))

    (texture-region [{:keys [graphics/textures]}
                     {:keys [image/file image/bounds]}]
      (assert file)
      (assert (contains? textures file))
      (let [texture (get textures file)]
        (if bounds
          (texture/region texture bounds)
          (texture/region texture)))))
  graphics)
