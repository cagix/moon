(ns cdq.graphics.draw-on-world-viewport
  (:require [cdq.graphics :as graphics]
            [com.badlogic.gdx.graphics.g2d.batch :as batch]
            [clojure.graphics.color :as color]
            [space.earlygrey.shape-drawer :as sd]))

(defn create [graphics]
  (extend-type (class graphics)
    graphics/DrawOnWorldViewport
    (draw-on-world-viewport! [{:keys [graphics/batch
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
        (reset! unit-scale world-unit-scale)
        (f)
        (reset! unit-scale 1))
      (batch/end! batch)))
  graphics)
