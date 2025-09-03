(ns cdq.tx.move-entity
  (:require [cdq.world :as w]
            [cdq.gdx.math.vector2 :as v]))

(defn do!
  [[_ eid body direction rotate-in-movement-direction?]
   {:keys [ctx/world]}]
  (w/context-entity-moved! world eid)
  (swap! eid assoc-in [:entity/body :body/position] (:body/position body))
  (when rotate-in-movement-direction?
    (swap! eid assoc-in [:entity/body :body/rotation-angle] (v/angle-from-vector direction)))
  nil)
