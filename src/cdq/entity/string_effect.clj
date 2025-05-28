(ns cdq.entity.string-effect
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :entity/string-effect
  (entity/tick! [[k {:keys [counter]}] eid ctx]
    (when (g/timer-stopped? ctx counter)
      [[:tx/dissoc eid k]]))

  (entity/render-above! [[_ {:keys [text]}]
                         entity
                         {:keys [ctx/graphics]}]
    (let [[x y] (entity/position entity)]
      [[:draw/text {:text text
                    :x x
                    :y (+ y
                          (:half-height entity)
                          (graphics/pixels->world-units graphics 5))
                    :scale 2
                    :up? true}]])))
