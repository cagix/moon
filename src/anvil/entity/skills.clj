(ns anvil.entity.skills
  (:refer-clojure :exclude [contains? remove])
  (:require [anvil.widgets.action-bar :refer [action-bar-add-skill
                                              action-bar-remove-skill]]))

(defn contains? [{:keys [entity/skills]} {:keys [property/id]}]
  (clojure.core/contains? skills id))

(defn add [c eid {:keys [property/id] :as skill}]
  {:pre [(not (contains? @eid skill))]}
  (when (:entity/player? @eid)
    (action-bar-add-skill c skill))
  (swap! eid assoc-in [:entity/skills id] skill))

(defn remove [c eid {:keys [property/id] :as skill}]
  {:pre [(contains? @eid skill)]}
  (when (:entity/player? @eid)
    (action-bar-remove-skill c skill))
  (swap! eid update :entity/skills dissoc id))
