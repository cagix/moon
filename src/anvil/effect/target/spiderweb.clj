(ns ^:no-doc anvil.effect.target.spiderweb
  (:require [anvil.entity :as entity]
            [cdq.context :refer [timer]]
            [clojure.component :as component :refer [defcomponent]]))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]

  (defcomponent :effects.target/spiderweb
    (component/info [_ _c]
      "Spiderweb slows 50% for 5 seconds."
      ; modifiers same like item/modifiers has info-text
      ; counter ?
      )

    (component/applicable? [_ _]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (component/handle [_ {:keys [effect/target]} c]
      (when-not (:entity/temp-modifier @target)
        (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                                   :counter (timer c duration)})
        (swap! target entity/mod-add modifiers)))))
