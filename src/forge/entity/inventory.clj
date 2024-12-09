(ns forge.entity.inventory
  (:require [anvil.inventory :as inventory]))

(defn create [[k items] eid]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (inventory/pickup-item eid item)))
