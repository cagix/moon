(ns cdq.effects.target.damage
  (:require [cdq.effect :as effect]
            [cdq.utils :refer [defmethods]]))

(defmethods :effects.target/damage
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         #_(:entity/hp @target))) ; not exist anymore ... bugfix .... -> is 'creature?'

  (effect/handle [[_ damage]
                  {:keys [effect/source effect/target]}
                  _world]
    [[:tx/deal-damage source target damage]]))
