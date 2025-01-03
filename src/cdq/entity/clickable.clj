(ns cdq.entity.clickable
  (:require [gdl.context :as c]))

(defn render-default [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity} c]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (c/draw-text c
                   {:text text
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}))))
