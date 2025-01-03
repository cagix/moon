(ns cdq.entity.inventory
  (:require [cdq.context :refer [pickup-item]]
            [cdq.entity :as entity]
            [cdq.inventory :as inventory]))

(defn create! [[k items] eid c]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (pickup-item c eid item)))
