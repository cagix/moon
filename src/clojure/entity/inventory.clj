(ns clojure.entity.inventory
  (:require [clojure.entity :as entity]
            [clojure.inventory :as inventory]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :entity/inventory
  (entity/create! [[k items] eid _ctx]
    (cons [:tx/assoc eid k inventory/empty-inventory]
          (for [item items]
            [:tx/pickup-item eid item]))))
