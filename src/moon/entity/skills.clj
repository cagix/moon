(ns moon.entity.skills
  (:require [moon.world.time :refer [stopped?]]))

(defn has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

(defn- add-skill [eid {:keys [property/id] :as skill}]
  (assert (not (has-skill? @eid skill)))
  [:e/assoc-in eid [:entity/skills id] skill])

(defn- remove-skill [eid {:keys [property/id] :as skill}]
  (assert (has-skill? @eid skill))
  [:e/dissoc-in eid [:entity/skills id]])

(defn create [[k skills] eid]
  (cons [:e/assoc eid k nil]
        (for [skill skills]
          [:entity/skills eid :add skill])))

(defn info [[_ skills]]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "[VIOLET]Skills: " (str/join "," (map name (keys skills))) "[]")))

(defn tick [[k skills] eid]
  (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
        :when (and cooling-down?
                   (stopped? cooling-down?))]
    [:e/assoc-in eid [k (:property/id skill) :skill/cooling-down?] false]))

(defn handle [[_ eid op skill]]
  [(case op
    :add    (add-skill    eid skill)
    :remove (remove-skill eid skill))
   (when (:entity/player? @eid)
     [:widgets/action-bar op skill])])
