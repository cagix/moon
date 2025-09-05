(ns cdq.render.draw-on-world-viewport
  (:require [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics.color :as color])
  (:import (com.badlogic.gdx.graphics.g2d Batch)))

(defn do!
  [{:keys [^Batch ctx/batch
           ctx/shape-drawer
           ctx/world-unit-scale
           ctx/unit-scale
           ctx/world-viewport]
    :as ctx}
   draw-fns]
  ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
  ; -> also Widgets, etc. ? check.
  (.setColor batch (color/->obj :white))
  (.setProjectionMatrix batch (:camera/combined (:viewport/camera world-viewport)))
  (.begin batch)
  (sd/with-line-width shape-drawer world-unit-scale
    (fn []
      (reset! unit-scale world-unit-scale)
      (doseq [f draw-fns]
        ((requiring-resolve f) ctx))
      (reset! unit-scale 1)))
  (.end batch))
