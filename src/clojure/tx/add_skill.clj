(ns clojure.tx.add-skill
  (:require [clojure.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/add-skill [[_ eid {:keys [property/id] :as skill}] ctx]
  {:pre [(not (contains? (:entity/skills @eid) id))]}
  (when (:entity/player? @eid)
    ((:skill-added! (:entity/player? @eid)) ctx skill))
  (swap! eid assoc-in [:entity/skills id] skill))
