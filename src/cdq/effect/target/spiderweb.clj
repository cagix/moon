(ns cdq.effect.target.spiderweb
  (:require [cdq.entity :as entity]
            [cdq.context :refer [timer]]))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]

  (defn info [_ _c]
    "Spiderweb slows 50% for 5 seconds."
    ; modifiers same like item/modifiers has info-text
    ; counter ?
    )

  (defn applicable? [_ _]
    ; ?
    true)

  ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
  (defn handle [_ {:keys [effect/target]} c]
    (when-not (:entity/temp-modifier @target)
      (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                                 :counter (timer c duration)})
      (swap! target entity/mod-add modifiers))))
