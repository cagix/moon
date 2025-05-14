(ns cdq.tx.move-entity
  (:require [cdq.ctx :as ctx]
            [cdq.math.vector2 :as v]
            [cdq.world :as world]))

(defn do! [eid body direction rotate-in-movement-direction?]
  (world/position-changed! ctx/world eid)
  (swap! eid assoc
         :position (:position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction))))
