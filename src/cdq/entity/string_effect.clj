(ns cdq.entity.string-effect
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.timer :as timer]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/string-effect
  (entity/tick! [[k {:keys [counter]}] eid {:keys [ctx/elapsed-time]}]
    (when (timer/stopped? elapsed-time counter)
      [[:tx/dissoc eid k]]))

  (entity/render-above! [[_ {:keys [text]}] entity ctx]
    (let [[x y] (:position entity)]
      (draw/text ctx
                 {:text text
                  :x x
                  :y (+ y
                        (:half-height entity)
                        (* 5 ctx/world-unit-scale))
                  :scale 2
                  :up? true}))))
