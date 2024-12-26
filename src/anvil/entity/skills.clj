(ns anvil.entity.skills
  (:refer-clojure :exclude [contains? remove])
  (:require [anvil.component :as component]
            [cdq.context :refer [stopped?]]))

(defn contains? [{:keys [entity/skills]} {:keys [property/id]}]
  (clojure.core/contains? skills id))

(declare player-add-skill
         player-remove-skill)

(defn add [entity {:keys [property/id] :as skill}]
  {:pre [(not (contains? entity skill))]}
  (when (:entity/player? entity)
    (player-add-skill skill))
  (assoc-in entity [:entity/skills id] skill))

(defn remove [entity {:keys [property/id] :as skill}]
  {:pre [(contains? entity skill)]}
  (when (:entity/player? entity)
    (player-remove-skill skill))
  (update entity :entity/skills dissoc id))

#_(defmethod component/info [skills _c]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "Skills: " (str/join "," (map name (keys skills))))))

(defmethods :entity/skills
  (component/create [[k skills] eid c]
    (swap! eid assoc k nil)
    (doseq [skill skills]
      (swap! eid add skill)))

  (component/tick [[k skills] eid c]
    (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
            :when (and cooling-down?
                       (stopped? c cooling-down?))]
      (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false))))
