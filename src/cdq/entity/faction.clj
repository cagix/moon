(ns cdq.entity.faction)

(defn info-text [[_ faction] _ctx]
  (str "Faction: " (name faction)))
