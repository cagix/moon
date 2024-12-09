(ns anvil.skills
  (:refer-clojure :exclude [contains? remove])
  (:require [anvil.action-bar :as action-bar]))

(defn contains? [{:keys [entity/skills]} {:keys [property/id]}]
  (clojure.core/contains? skills id))

(defn add [entity {:keys [property/id] :as skill}]
  {:pre [(not (contains? entity skill))]}
  (when (:entity/player? entity)
    (action-bar/add-skill skill))
  (assoc-in entity [:entity/skills id] skill))

(defn remove [entity {:keys [property/id] :as skill}]
  {:pre [(contains? entity skill)]}
  (when (:entity/player? entity)
    (action-bar/remove-skill skill))
  (update entity :entity/skills dissoc id))
