(ns cdq.entity.string-effect
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :entity/string-effect
  (entity/tick! [[k {:keys [counter]}] eid ctx]
    (when (g/timer-stopped? ctx counter)
      [[:tx/dissoc eid k]]))

  (entity/render-above! [[_ {:keys [text]}]
                         entity
                         ctx]
    (let [[x y] (entity/position entity)]
      [[:draw/text {:text text
                    :x x
                    :y (+ y
                          (:half-height entity)
                          (g/pixels->world-units ctx 5))
                    :scale 2
                    :up? true}]])))
