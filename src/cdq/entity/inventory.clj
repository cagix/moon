(ns cdq.entity.inventory
  (:require [cdq.entity :as entity]
            [cdq.inventory :as inventory]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/inventory
  (entity/create! [[k items] eid]
    (cons [:tx/assoc eid k inventory/empty-inventory]
          (for [item items]
            [:tx/pickup-item eid item]))))
