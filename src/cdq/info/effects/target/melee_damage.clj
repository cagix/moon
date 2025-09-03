(ns cdq.info.effects.target.melee-damage)

; FIXME no source
; => to entity move
(defn info-segment [_ _ctx]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))
