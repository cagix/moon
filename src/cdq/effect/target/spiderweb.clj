(ns cdq.effect.target.spiderweb
  (:require [cdq.entity :as entity]
            [gdl.context.timer :as timer]))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]
  (defn applicable? [_ _]
    ; ?
    true)

  ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
  (defn handle [_ {:keys [effect/target]} c]
    (when-not (:entity/temp-modifier @target)
      (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                                 :counter (timer/create c duration)})
      (swap! target entity/mod-add modifiers))))
