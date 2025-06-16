(ns cdq.effects.target.spiderweb
  (:require [cdq.effect :as effect]
            [cdq.timer :as timer]
            [cdq.utils :refer [defmethods]]))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]
  (defmethods :effects.target/spiderweb
    (effect/applicable? [_ _]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (effect/handle [_
                    {:keys [effect/target]}
                    {:keys [ctx/world]}]
      (when-not (:entity/temp-modifier @target)
        [[:tx/assoc target :entity/temp-modifier {:modifiers modifiers
                                                  :counter (timer/create (:world/elapsed-time world) duration)}]
         [:tx/mod-add target modifiers]]))))
