(ns cdq.ctx.world
  (:require [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]))

(declare entity-components)

(defn context-entity-add!
  [{:keys [world/entity-ids
           world/content-grid
           world/grid]}
   eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))
  (content-grid/add-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid))
  (grid/add-entity! grid eid))

(defn context-entity-remove!
  [{:keys [world/entity-ids
           world/grid]}
   eid]
  (let [id (:entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))
  (content-grid/remove-entity! eid)
  (grid/remove-entity! grid eid))

(defn context-entity-moved!
  [{:keys [world/content-grid
           world/grid]}
   eid]
  (content-grid/position-changed! content-grid eid)
  (grid/position-changed! grid eid))
