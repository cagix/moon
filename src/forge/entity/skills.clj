(ns forge.entity.skills
  (:require [anvil.skills :as skills]
            [anvil.world :refer [stopped?]]))

(defn create [[k skills] eid]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (swap! eid skills/add skill)))

(defn tick [[k skills] eid]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (stopped? cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))
