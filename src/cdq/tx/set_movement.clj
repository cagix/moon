(ns cdq.tx.set-movement
  (:require [cdq.world.entity :as entity]))

(defn- set-movement [entity movement-vector]
  (assoc entity :entity/movement {:direction movement-vector
                                  :speed (or (entity/stat entity :entity/movement-speed) 0)}))

(defn do! [[_ eid movement-vector] _ctx]
  (swap! eid set-movement movement-vector)
  nil)
