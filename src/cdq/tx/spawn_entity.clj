(ns cdq.tx.spawn-entity
  (:require [cdq.entity :as entity]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [cdq.malli :as m]
            [qrecord.core :as q]))

(q/defrecord Entity [entity/body])

(defn do!
  [{:keys [ctx/world]
    :as ctx}
   entity]
  (let [{:keys [world/content-grid
                world/entity-ids
                world/grid
                world/id-counter
                world/spawn-entity-schema
                ]} world
        _ (m/validate-humanize spawn-entity-schema entity)
        entity (reduce (fn [m [k v]]
                         (assoc m k (entity/create [k v] ctx)))
                       {}
                       entity)
        _ (assert (and (not (contains? entity :entity/id))))
        entity (assoc entity :entity/id (swap! id-counter inc))
        entity (merge (map->Entity {}) entity)
        eid (atom entity)]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/add-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid))
    (grid/set-touched-cells! grid eid)
    (when (:body/collides? (:entity/body @eid))
      (grid/set-occupied-cells! grid eid))
    (mapcat #(entity/create! % eid ctx) @eid)))
