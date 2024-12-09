(ns forge.entity.skills
  (:require [anvil.entity :as entity]
            [anvil.world :refer [stopped?]]))

(defn create [[k skills] eid]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (swap! eid entity/add-skill skill)))

(defn tick [[k skills] eid]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (stopped? cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))
