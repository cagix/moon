(ns clojure.tx.remove-skill
  (:require [clojure.ctx.effect-handler :refer [do!]]))

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (when (:entity/player? @eid)
      ((:skill-removed! (:entity/player? @eid)) ctx skill))
    (swap! eid update :entity/skills dissoc id))

