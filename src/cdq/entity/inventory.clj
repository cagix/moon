(ns cdq.entity.inventory
  (:require [cdq.inventory :as inventory]))

(defn create! [items eid _world]
  (cons [:tx/assoc eid :entity/inventory inventory/empty-inventory]
        (for [item items]
          [:tx/pickup-item eid item])))
