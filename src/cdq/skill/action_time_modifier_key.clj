(ns cdq.skill.action-time-modifier-key)

(defn info-text [[_ v] _world]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))
