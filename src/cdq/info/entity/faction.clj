(ns cdq.info.entity.faction)

(defn info-segment [[_ faction] _ctx]
  (str "Faction: " (name faction)))
