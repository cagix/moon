(ns cdq.effects.target.spiderweb
  (:require [cdq.entity.stats :as stats]
            [gdl.timer :as timer]))

(def modifiers {:modifier/movement-speed {:op/mult -50}})
(def duration 5)

(defn applicable? [_ {:keys [effect/target]}]
  ; TODO has stats , for mod-add
  ; e,g, spiderweb on projectile leads to error
  (:entity/stats @target))

; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
(defn handle [_
              {:keys [effect/target]}
              world]
  (let [{:keys [world/elapsed-time]} world]
    (when-not (:entity/temp-modifier @target)
      [[:tx/assoc target :entity/temp-modifier {:modifiers modifiers
                                                :counter (timer/create elapsed-time duration)}]
       [:tx/update target :entity/stats stats/add modifiers]])))
