(ns clojure.tx.move-entity
  (:require [clojure.ctx :as ctx]
            [clojure.ctx.effect-handler :refer [do!]]
            [clojure.vector2 :as v]))

(defmethod do! :tx/move-entity [[_ eid body direction rotate-in-movement-direction?] ctx]
  (ctx/context-entity-moved! ctx eid)
  (swap! eid assoc
         :entity/position (:entity/position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction))))
