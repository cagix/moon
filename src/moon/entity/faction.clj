(ns moon.entity.faction)

(defn info [faction]
  (str "[SLATE]Faction: " (name faction) "[]"))

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))
