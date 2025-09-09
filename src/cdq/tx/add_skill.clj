(ns cdq.tx.add-skill
  (:require [cdq.skills :as skills]))

(defn do! [[_ eid skill] _ctx]
  (swap! eid update :entity/skills skills/add-skill skill)
  (if (:entity/player? @eid)
    [[:tx/player-add-skill skill]]
    nil))

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (swap! eid update :entity/skills dissoc id)
    (when (:entity/player? @eid)
      (remove-skill! ctx skill)))
