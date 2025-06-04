(ns cdq.tx.add-skill
  (:require [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/add-skill [[_ eid {:keys [property/id] :as skill}] ctx]
  {:pre [(not (contains? (:entity/skills @eid) id))]}
  (swap! eid assoc-in [:entity/skills id] skill)
  (when (:entity/player? @eid)
    ((:skill-added! (:entity/player? @eid)) ctx skill))
  (when (:entity/player? @eid)
    [:world.event/player-skill-added (:property/id skill)]))
