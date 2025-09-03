(ns cdq.info.skill.action-time-modifier-key)

(defn info-segment [[_ v] _ctx]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))
