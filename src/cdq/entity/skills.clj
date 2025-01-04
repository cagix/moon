(ns cdq.entity.skills
  (:require [anvil.entity.skills :as skills]
            [gdl.context.timer :as timer]))

(defn create! [[k skills] eid c]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (skills/add c eid skill)))

(defn tick [[k skills] eid c]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (timer/stopped? c cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))
