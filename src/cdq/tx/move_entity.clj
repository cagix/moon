(ns cdq.tx.move-entity
  (:require [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [cdq.gdx.math.vector2 :as v]))

(defn do!
  [[_ eid body direction rotate-in-movement-direction?]
   {:keys [ctx/content-grid
           ctx/world]}]
  (content-grid/position-changed! content-grid eid)
  (grid/position-changed! (:world/grid world) eid)
  (swap! eid assoc-in [:entity/body :body/position] (:body/position body))
  (when rotate-in-movement-direction?
    (swap! eid assoc-in [:entity/body :body/rotation-angle] (v/angle-from-vector direction)))
  nil)
