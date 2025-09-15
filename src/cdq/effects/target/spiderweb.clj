(ns cdq.effects.target.spiderweb
  (:require [cdq.timer :as timer]))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]

  (defn applicable? [_ _]
    ; ?
    true)

  ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
  (defn handle [_
                {:keys [effect/target]}
                {:keys [ctx/world]}]
    (let [{:keys [world/elapsed-time]} world]
      (when-not (:entity/temp-modifier @target)
        [[:tx/assoc target :entity/temp-modifier {:modifiers modifiers
                                                  :counter (timer/create elapsed-time duration)}]
         [:tx/mod-add target modifiers]]))))

(defn info-text [_ _ctx]
  "Spiderweb slows 50% for 5 seconds.")
