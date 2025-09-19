(ns cdq.application.create.world.record
  (:require cdq.application.create.world))

(defn do! [world]
  (merge (cdq.application.create.world/map->World {}) world))
