(ns cdq.tx.add-skill)

(defn do! [_ctx eid {:keys [property/id] :as skill}]
  {:pre [(not (contains? (:entity/skills @eid) id))]}
  (swap! eid update :entity/skills assoc id skill)
  (if (:entity/player? @eid)
    [[:tx/player-add-skill skill]]
    nil))

#_(defn remove-skill [_ctx eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (swap! eid update :entity/skills dissoc id)
    (when (:entity/player? @eid)
      (remove-skill! ctx skill)))
