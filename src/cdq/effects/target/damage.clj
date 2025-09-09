(ns cdq.effects.target.damage
  (:require [cdq.damage :as damage]))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       #_(:entity/hp @target))) ; not exist anymore ... bugfix .... -> is 'creature?'

(defn handle [[_ damage]
              {:keys [effect/source effect/target]}
              _ctx]
  [[:tx/deal-damage source target damage]])

(defn info-text [[_ damage] _ctx]
  (damage/info-text damage)
  #_(if source
      (let [modified (stats/damage @source damage)]
        (if (= damage modified)
          (damage/info-text damage)
          (str (damage/info-text damage) "\nModified: " (damage/info modified))))
      (damage/info-text damage)) ; property menu no source,modifiers
  )
