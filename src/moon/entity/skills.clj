(ns moon.entity.skills
  (:require [gdl.system :refer [*k*]]
            [moon.effect :as effect]
            [moon.entity.mana :as mana]
            [moon.widgets.action-bar :as action-bar]
            [moon.world.time :refer [stopped?]]))

(defn has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

(defn- add-skill [entity {:keys [property/id] :as skill}]
  (assert (not (has-skill? entity skill)))
  (assoc-in entity [:entity/skills id] skill))

(defn- remove-skill [entity {:keys [property/id] :as skill}]
  (assert (has-skill? entity skill))
  (update entity :entity/skills dissoc id))

(defn create [skills eid]
  (swap! eid assoc *k* nil)
  (for [skill skills]
    [:entity/skills eid :add skill]))

(defn info [skills]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "[VIOLET]Skills: " (str/join "," (map name (keys skills))) "[]")))

(defn tick [skills eid]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (stopped? cooling-down?))]
    (swap! eid assoc-in [*k* (:property/id skill) :skill/cooling-down?] false)))

(defn handle [eid op skill]
  (when (:entity/player? @eid)
    (case op
      :add    (action-bar/add-skill    skill)
      :remove (action-bar/remove-skill skill)))
  (swap! eid (case op
               :add    add-skill
               :remove remove-skill)
         skill)
  nil)

(defn- mana-value [entity]
  (if-let [mana (:entity/mana entity)]
    ((mana/value entity) 0)
    0))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana-value entity))))

(defn usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill}]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effect/applicable? effects))
   :invalid-params

   :else
   :usable))
