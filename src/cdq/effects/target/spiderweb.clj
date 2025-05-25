(ns cdq.effects.target.spiderweb
  (:require [cdq.effect :as effect]
            [cdq.g :as g]
            [gdl.utils :refer [defcomponent]]))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]
  (defcomponent :effects.target/spiderweb
    (effect/applicable? [_ _]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (effect/handle [_ {:keys [effect/target]} ctx]
      (when-not (:entity/temp-modifier @target)
        [[:tx/assoc target :entity/temp-modifier {:modifiers modifiers
                                                  :counter (g/create-timer ctx duration)}]
         [:tx/mod-add target modifiers]]))))
