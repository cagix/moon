(ns cdq.graphics
  (:require [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.g2d.batch :as batch]))

(defn draw-on-world-viewport!
  [{:keys [ctx/batch
           ctx/shape-drawer
           ctx/unit-scale
           ctx/world-unit-scale
           ctx/world-viewport]}
   draw!]
  ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
  ; -> also Widgets, etc. ? check.
  (batch/set-color! batch color/white)
  (batch/set-projection-matrix! batch (:camera/combined (:viewport/camera world-viewport)))
  (batch/begin! batch)
  (sd/with-line-width shape-drawer world-unit-scale
    (fn []
      (reset! unit-scale world-unit-scale)
      (draw!)
      (reset! unit-scale 1)))
  (batch/end! batch))

(defn- draw!
  [{k 0 :as component}
   {:keys [ctx/draw-fns]
    :as ctx}]
  (let [draw-fn (draw-fns k)]
    (draw-fn component ctx)))

(defn handle-draws! [ctx draws]
  (doseq [component draws
          :when component]
    (draw! component ctx)))
