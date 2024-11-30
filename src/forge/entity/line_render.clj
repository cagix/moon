(ns ^:no-doc forge.entity.line-render
  (:require [forge.graphics :refer [draw-line with-line-width]]))

(defn render [{:keys [thick? end color]} entity]
  (let [position (:position entity)]
    (if thick?
      (with-line-width 4
        #(draw-line position end color))
      (draw-line position end color))))
