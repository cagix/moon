(ns cdq.tx.move-entity
  (:require [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]
            [cdq.vector2 :as v]))

(defn do! [{:keys [ctx/content-grid
                   ctx/grid]}
           eid body direction rotate-in-movement-direction?]
  (content-grid/position-changed! content-grid eid)
  (grid/position-changed! grid eid)
  (swap! eid assoc
         :position (:position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction))))
