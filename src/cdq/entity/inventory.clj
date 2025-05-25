(ns cdq.entity.inventory
  (:require [cdq.entity :as entity]
            [cdq.inventory :as inventory]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :entity/inventory
  (entity/create! [[k items] eid _ctx]
    (cons [:tx/assoc eid k inventory/empty-inventory]
          (for [item items]
            [:tx/pickup-item eid item]))))
