(ns cdq.skill.cost)

(defn info-text [[_ v] _ctx]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))
