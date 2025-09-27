(ns cdq.skill.action-time-modifier-key)

(defn info-text [[_ v] _world]
  (case v
    :stats/cast-speed "Spell"
    :stats/attack-speed "Attack"))
