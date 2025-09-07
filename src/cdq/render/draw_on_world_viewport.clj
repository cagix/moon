(ns cdq.render.draw-on-world-viewport
  (:require [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.g2d.batch :as batch]))

(defn do!
  [{:keys [ctx/draw-on-world-viewport
           ctx/batch
           ctx/shape-drawer
           ctx/unit-scale
           ctx/world-unit-scale
           ctx/world-viewport]
    :as ctx}]
  ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
  ; -> also Widgets, etc. ? check.
  (batch/set-color! batch color/white)
  (batch/set-projection-matrix! batch (:camera/combined (:viewport/camera world-viewport)))
  (batch/begin! batch)
  (sd/with-line-width shape-drawer world-unit-scale
    (fn []
      (reset! unit-scale world-unit-scale)
      (doseq [f draw-on-world-viewport]
        (f ctx))
      (reset! unit-scale 1)))
  (batch/end! batch))
