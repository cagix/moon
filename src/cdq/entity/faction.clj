(ns cdq.entity.faction)

(defn info-text [[_ faction] _ctx]
  (str "Faction: " (name faction)))

(defn enemy [faction]
  (case faction
    :evil :good
    :good :evil))
