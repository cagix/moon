(ns cdq.entity.skills
  (:require [cdq.timer :as timer]))

(defn create! [skills eid _ctx]
  (cons [:tx/assoc eid :entity/skills nil]
        (for [skill skills]
          [:tx/add-skill eid skill])))

(defn tick! [skills eid {:keys [ctx/elapsed-time]}]
  (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
        :when (and cooling-down?
                   (timer/stopped? elapsed-time cooling-down?))]
    [:tx/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] false]))
