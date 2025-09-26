(ns cdq.skill.cost)

(defn info-text [[_ v] _world]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))
