(ns cdq.entity.inventory
  (:require [clojure.grid2d :as g2d]
            [cdq.inventory :as inventory]))

(defn- create-inventory []
  (->> inventory/empty-inventory
       (map (fn [[slot [width height]]]
              [slot (g2d/create-grid width height (constantly nil))]))
       (into {})))

(defn create! [items eid _ctx]
  (cons [:tx/assoc eid :entity/inventory (create-inventory)]
        (for [item items]
          [:tx/pickup-item eid item])))
