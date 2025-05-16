(ns cdq.tx.move-entity
  (:require [cdq.ctx :as ctx]
            [cdq.vector2 :as v]
            [cdq.impl.world]))

(defn do! [eid body direction rotate-in-movement-direction?]
  (cdq.impl.world/position-changed! eid)
  (swap! eid assoc
         :position (:position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction))))
