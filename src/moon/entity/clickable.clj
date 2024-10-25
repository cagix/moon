(ns moon.entity.clickable
  (:require [moon.component :refer [defc]]
            [moon.entity :as entity]
            [moon.graphics :as g]))

(defc :entity/clickable
  (entity/render [[_ {:keys [text]}]
                  {:keys [entity/mouseover?] :as entity}]
    (when (and mouseover? text)
      (let [[x y] (:position entity)]
        (g/draw-text {:text text
                      :x x
                      :y (+ y (:half-height entity))
                      :up? true})))))
