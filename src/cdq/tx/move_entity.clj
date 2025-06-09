(ns cdq.tx.move-entity
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.vector2 :as v]
            [cdq.world :as world]))

(defmethod do! :tx/move-entity [[_ eid body direction rotate-in-movement-direction?] ctx]
  (world/context-entity-moved! ctx eid)
  (swap! eid assoc
         :entity/position (:entity/position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))
  nil)
