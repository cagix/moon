(ns moon.entity.skills
  (:require [moon.component :as component]
            [moon.entity :as entity]
            [moon.world.time :refer [stopped?]]))

(defn has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

(defn- add-skill [eid {:keys [property/id] :as skill}]
  (assert (not (has-skill? @eid skill)))
  [[:e/assoc-in eid [:entity/skills id] skill]
   (when (:entity/player? @eid)
     [:tx.action-bar/add skill])])

(defn- remove-skill [eid {:keys [property/id] :as skill}]
  (assert (has-skill? @eid skill))
  [[:e/dissoc-in eid [:entity/skills id]]
   (when (:entity/player? @eid)
     [:tx.action-bar/remove skill])])

(defmethods :entity/skills
  (entity/create [[k skills] eid]
    (cons [:e/assoc eid k nil]
          (for [skill skills]
            [:entity/skills eid :add skill])))

  (component/info [[_ skills]]
    ; => recursive info-text leads to endless text wall
    #_(when (seq skills)
        (str "[VIOLET]Skills: " (str/join "," (map name (keys skills))) "[]")))

  (entity/tick [[k skills] eid]
    (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (stopped? cooling-down?))]
      [:e/assoc-in eid [k (:property/id skill) :skill/cooling-down?] false]))

  (component/handle [[_ eid add-or-remove skill]]
    (case add-or-remove
      :add    (add-skill    eid skill)
      :remove (remove-skill eid skill))))

