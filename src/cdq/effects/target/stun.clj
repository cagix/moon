(ns cdq.effects.target.stun
  (:require [cdq.effect :as effect]
            [cdq.utils :refer [defmethods]]))

(defmethods :effects.target/stun
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [[_ duration] {:keys [effect/target]} _world]
    [[:tx/event target :stun duration]]))
