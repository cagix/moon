(ns cdq.entity.clickable
  (:require [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/clickable
  (entity/render-default! [[_ {:keys [text]}]
                           {:keys [entity/mouseover?] :as entity}]
    (when (and mouseover? text)
      (let [[x y] (:position entity)]
        (draw/text {:text text
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true})))))
