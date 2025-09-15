(ns cdq.tx.spawn-entity
  (:require [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [cdq.malli :as m]
            [qrecord.core :as q]))

(q/defrecord Entity [entity/body])

(defn do!
  [{:keys [
           ctx/entity-ids
           ctx/entity-components
           ctx/spawn-entity-schema
           ctx/world]
    :as ctx}
   entity]
  (m/validate-humanize spawn-entity-schema entity)
  (let [{:keys [world/content-grid
                world/grid
                world/id-counter
                ]} world
        build-component (fn [[k v]]
                          (if-let [create (k (:create entity-components))]
                            (create v ctx)
                            v))
        entity (reduce (fn [m [k v]]
                         (assoc m k (build-component [k v])))
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
    (mapcat (fn [[k v]]
              (when-let [create! (k (:create! entity-components))]
                (create! v eid ctx)))
            @eid)))
