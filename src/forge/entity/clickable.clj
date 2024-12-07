(ns forge.entity.clickable
  (:require [forge.entity :refer [render-default]]
            [forge.graphics :refer [draw-text]]))

(defmethod render-default :entity/clickable
  [[_ {:keys [text]}]
   {:keys [entity/mouseover?] :as entity}]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (draw-text {:text text
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))
