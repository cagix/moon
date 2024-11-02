(ns moon.entity.clickable
  (:require [gdl.graphics.text :as text]
            [moon.entity :as entity]))

(defmethods :entity/clickable
  (entity/render [[_ {:keys [text]}]
                  {:keys [entity/mouseover?] :as entity}]
    (when (and mouseover? text)
      (let [[x y] (:position entity)]
        (text/draw {:text text
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true})))))
