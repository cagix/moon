(ns cdq.entity.faction)

(defn info [[_ faction] _entity _c]
  (str "Faction: " (name faction)))
