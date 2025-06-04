(ns cdq.tx.remove-skill
  (:require [cdq.ctx.effect-handler :refer [do!]]))

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (when (:entity/player? @eid)
      ((:skill-removed! (:entity/player? @eid)) ctx skill))
    (swap! eid update :entity/skills dissoc id)
    nil)

