(ns cdq.entity.skills
  (:require [gdl.context.timer :as timer]))

(defn tick [[k skills] eid c]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (timer/stopped? c cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))
