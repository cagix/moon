(ns cdq.tx.move-entity
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]
            [cdq.vector2 :as v]))

(defn do! [eid body direction rotate-in-movement-direction?]
  (content-grid/position-changed! ctx/content-grid eid)
  (grid/position-changed! ctx/grid eid)
  (swap! eid assoc
         :position (:position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction))))
