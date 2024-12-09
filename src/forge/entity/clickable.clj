(ns forge.entity.clickable
  (:require [anvil.graphics :refer [draw-text]]))

(defn render-default [[_ {:keys [text]}]
   {:keys [entity/mouseover?] :as entity}]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (draw-text {:text text
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))
