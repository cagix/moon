(ns cdq.entity.skills
  (:require [cdq.timer :as timer]
            [clojure.string :as str]))

(defn create! [skills eid _ctx]
  (cons [:tx/assoc eid :entity/skills nil]
        (for [skill skills]
          [:tx/add-skill eid skill])))

(defn tick! [skills eid {:keys [ctx/world]}]
  (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
        :when (and cooling-down?
                   (timer/stopped? (:world/elapsed-time world) cooling-down?))]
    [:tx/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] false]))

(defn info-text [[_ skills] _ctx]
  ; => recursive info-text leads to endless text wall
  (when (seq skills)
    (str "Skills: " (str/join "," (map name (keys skills))))))

(defn add-skill
  [skills
   {:keys [property/id] :as skill}]
  {:pre [(not (contains? skills id))]}
  (assoc skills id skill))
