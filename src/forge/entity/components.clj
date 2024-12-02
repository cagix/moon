(ns forge.entity.components
  (:require [forge.core :refer :all]
            [forge.ui.action-bar :as action-bar]))

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
