(ns cdq.tx.remove-skill
  (:require [cdq.ctx.effect-handler :refer [do!]]))

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (swap! eid update :entity/skills dissoc id)
    (when (:entity/player? @eid)
      [:world.event/player-skill-removed skill]))

