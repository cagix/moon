(ns anvil.entity.skills
  (:refer-clojure :exclude [contains? remove])
  (:require [anvil.entity :as entity]
            #_[anvil.info :as info]
            [anvil.widgets.action-bar :refer [action-bar-add-skill
                                              action-bar-remove-skill]]
            [cdq.context :refer [stopped?]]
            [clojure.utils :refer [defmethods]]))

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

#_(defmethod info/segment [skills _c]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "Skills: " (str/join "," (map name (keys skills))))))

(defmethods :entity/skills
  (entity/create [[k skills] eid c]
    (swap! eid assoc k nil)
    (doseq [skill skills]
      (add c eid skill)))

  (entity/tick [[k skills] eid c]
    (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
            :when (and cooling-down?
                       (stopped? c cooling-down?))]
      (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false))))
