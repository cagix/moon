(ns cdq.application.create.world.record)

(defrecord World [])

(defn do! [world]
  (merge (map->World {}) world))
