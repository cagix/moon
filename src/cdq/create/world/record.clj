(ns cdq.create.world.record)

(defrecord World [])

(defn do! [world]
  (merge (map->World {}) world))
