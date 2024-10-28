(ns moon.entity.clickable
  (:require [moon.entity :as entity]
            [moon.graphics.text :as text]))

(defc :entity/clickable
  (entity/render [[_ {:keys [text]}]
                  {:keys [entity/mouseover?] :as entity}]
    (when (and mouseover? text)
      (let [[x y] (:position entity)]
        (text/draw {:text text
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true})))))
