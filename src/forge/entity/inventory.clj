(ns forge.entity.inventory
  (:require [anvil.entity :as entity]
            [anvil.inventory :as inventory]))

(defn create [[k items] eid]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (entity/pickup-item eid item)))
