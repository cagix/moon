(ns forge.effects.target.spiderweb
  (:require [anvil.modifiers :as mods]
            [anvil.time :refer [timer]]))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]

  (defn applicable? [_ _]
    ; ?
    true)

  ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
  (defn handle [_ {:keys [effect/target]}]
    (when-not (:entity/temp-modifier @target)
      (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                                 :counter (timer duration)})
      (swap! target mods/add modifiers))))
