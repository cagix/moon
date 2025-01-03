(ns cdq.entity.inventory
  (:require [cdq.entity :as entity]
            [cdq.inventory :as inventory]))

(defn create! [[k items] eid c]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (entity/pickup-item c eid item)))
