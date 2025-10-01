(ns cdq.entity.skills.tick
  (:require [cdq.timer :as timer]))

(defn txs [skills eid {:keys [world/elapsed-time]}]
  (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
        :when (and cooling-down?
                   (timer/stopped? elapsed-time cooling-down?))]
    [:tx/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] false]))
