(ns cdq.info.skill.cost)

(defn info-segment [[_ v] _ctx]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))
