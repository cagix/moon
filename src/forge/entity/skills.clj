(ns forge.entity.skills
  (:require [anvil.action-bar :as action-bar]
            [anvil.world :refer [stopped?]]))

(defn has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

(defn add-skill [entity {:keys [property/id] :as skill}]
  {:pre [(not (has-skill? entity skill))]}
  (when (:entity/player? entity)
    (action-bar/add-skill skill))
  (assoc-in entity [:entity/skills id] skill))

(defn remove-skill [entity {:keys [property/id] :as skill}]
  {:pre [(has-skill? entity skill)]}
  (when (:entity/player? entity)
    (action-bar/remove-skill skill))
  (update entity :entity/skills dissoc id))

(defn create [[k skills] eid]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (swap! eid add-skill skill)))

(defn tick [[k skills] eid]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (stopped? cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))
