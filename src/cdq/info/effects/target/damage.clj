(ns cdq.info.effects.target.damage)

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(defn info-segment [[_ damage] _ctx]
  (damage-info damage)
  #_(if source
      (let [modified (modifiers/damage @source damage)]
        (if (= damage modified)
          (damage-info damage)
          (str (damage-info damage) "\nModified: " (damage/info modified))))
      (damage-info damage)) ; property menu no source,modifiers
  )
