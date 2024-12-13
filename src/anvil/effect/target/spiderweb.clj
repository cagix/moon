(ns anvil.effect.target.spiderweb
  (:require [anvil.component :as component]
            [anvil.entity.modifiers :as mods]
            [anvil.world :refer [timer]]
            [gdl.utils :refer [defmethods]]))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]

  (defmethods :effects.target/spiderweb
    (component/info [_]
      "Spiderweb slows 50% for 5 seconds."
      ; modifiers same like item/modifiers has info-text
      ; counter ?
      )

    (component/applicable? [_ _]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (component/handle [_ {:keys [effect/target]}]
      (when-not (:entity/temp-modifier @target)
        (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                                   :counter (timer duration)})
        (swap! target mods/add modifiers)))))
