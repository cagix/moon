(ns cdq.entity.skills
  (:require [cdq.entity :as entity]
            [cdq.timer :as timer]
            [cdq.utils :refer [defmethods]]))

(defn tick! [[k skills] eid {:keys [ctx/elapsed-time]}]
  (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
        :when (and cooling-down?
                   (timer/stopped? elapsed-time cooling-down?))]
    [:tx/assoc-in eid [k (:property/id skill) :skill/cooling-down?] false]))

(defmethods :entity/skills
  (entity/create! [[k skills] eid _ctx]
    (cons [:tx/assoc eid k nil]
          (for [skill skills]
            [:tx/add-skill eid skill]))))
