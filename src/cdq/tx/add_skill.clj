(ns cdq.tx.add-skill
  (:require [cdq.ctx.effect-handler :refer [do!]]))

(defn- add-skill [skills {:keys [property/id] :as skill}]
  {:pre [(not (contains? skills id))]}
  (assoc skills id skill))

(defmethod do! :tx/add-skill [[_ eid skill] ctx]
  (swap! eid update :entity/skills add-skill skill)
  (when (:entity/player? @eid)
    [:world.event/player-skill-added skill]))
