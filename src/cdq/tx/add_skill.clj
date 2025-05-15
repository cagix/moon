(ns cdq.tx.add-skill)

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (when (:entity/player? @eid)
      ((:skill-removed! (:entity/player? @eid)) skill))
    (swap! eid update :entity/skills dissoc id))

(defn do! [eid {:keys [property/id] :as skill}]
  {:pre [(not (contains? (:entity/skills @eid) id))]}
  (when (:entity/player? @eid)
    ((:skill-added! (:entity/player? @eid)) skill))
  (swap! eid assoc-in [:entity/skills id] skill))
