(ns cdq.effects.target.stun
  (:require [cdq.effect :as effect]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :effects.target/stun
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [[_ duration] {:keys [effect/target]} _ctx]
    [[:tx/event target :stun duration]]))
