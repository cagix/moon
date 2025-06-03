(ns cdq.tx.move-entity
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.effect-handler :refer [do!]]
            [cdq.vector2 :as v]))

(defmethod do! :tx/move-entity [[_ eid body direction rotate-in-movement-direction?] ctx]
  (ctx/context-entity-moved! ctx eid)
  (swap! eid assoc
         :entity/position (:entity/position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction))))
