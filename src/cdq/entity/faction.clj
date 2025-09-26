(ns cdq.entity.faction)

(defn enemy [faction]
  (case faction
    :evil :good
    :good :evil))

(defn info-text [[_ faction] _world]
  (str "Faction: " (name faction)))
