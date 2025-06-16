(ns cdq.effects.target.kill
  (:require [cdq.effect :as effect]
            [cdq.utils :refer [defmethods]]))

(defmethods :effects.target/kill
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [_ {:keys [effect/target]} _world]
    [[:tx/event target :kill]]))
