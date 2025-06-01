(ns cdq.tx.move-entity
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.world :as world]
            [cdq.vector2 :as v]))

(defmethod do! :tx/move-entity [[_ eid body direction rotate-in-movement-direction?] ctx]
  (world/context-entity-moved! ctx eid)
  (swap! eid assoc
         :position (:position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction))))
