(ns cdq.entity.faction)

(defn text [[_ faction] _entity _c]
  (str "Faction: " (name faction)))
