(ns moon.entity.skills
  (:require [gdl.system :refer [*k*]]
            [moon.effect :as effect]
            [moon.entity.mana :as mana]
            [moon.widgets.action-bar :as action-bar]
            [moon.world.time :refer [stopped?]]))

(defn has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

(defn- add-skill [eid {:keys [property/id] :as skill}]
  (assert (not (has-skill? @eid skill)))
  [:e/assoc-in eid [:entity/skills id] skill])

(defn- remove-skill [eid {:keys [property/id] :as skill}]
  (assert (has-skill? @eid skill))
  [:e/dissoc-in eid [:entity/skills id]])

(defn create [skills eid]
  (cons [:e/assoc eid *k* nil]
        (for [skill skills]
          [:entity/skills eid :add skill])))

(defn info [skills]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "[VIOLET]Skills: " (str/join "," (map name (keys skills))) "[]")))

(defn tick [skills eid]
  (doall (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
               :when (and cooling-down?
                          (stopped? cooling-down?))]
           [:e/assoc-in eid [*k* (:property/id skill) :skill/cooling-down?] false])))

(defn handle [eid op skill]
  (when (:entity/player? @eid)
    (case op
      :add    (action-bar/add-skill    skill)
      :remove (action-bar/remove-skill skill)))
  [(case op
    :add    (add-skill    eid skill)
    :remove (remove-skill eid skill))])

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
