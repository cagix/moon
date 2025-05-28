(ns cdq.entity.string-effect
  (:require [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.timer :as timer]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :entity/string-effect
  (entity/tick! [[k {:keys [counter]}] eid {:keys [ctx/elapsed-time]}]
    (when (timer/stopped? elapsed-time counter)
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
