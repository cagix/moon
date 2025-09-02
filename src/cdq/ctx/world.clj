(ns cdq.ctx.world
  (:require [cdq.raycaster :as raycaster]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.entity :as entity]
            [cdq.world.grid :as grid]
            [cdq.world.grid.cell :as cell]
            [cdq.world.potential-fields.movement :as potential-fields.movement]
            [cdq.world.potential-fields.update :as potential-fields.update]))

(declare entity-components)

(defn context-entity-add!
  [{:keys [world/entity-ids
           world/content-grid
           world/grid]}
   eid]
  (let [id (entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))
  (content-grid/add-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid))
  (grid/add-entity! grid eid))

(defn- context-entity-remove! [{:keys [world/entity-ids
                                       world/grid]}
                               eid]
  (let [id (entity/id @eid)]
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

(defn update-time [{:keys [world/max-delta] :as world} delta-ms]
  (let [delta-ms (min delta-ms max-delta)]
    (-> world
        (assoc :world/delta-time delta-ms)
        (update :world/elapsed-time + delta-ms))))

(defn path-blocked? [{:keys [world/raycaster]} start end width]
  (raycaster/path-blocked? raycaster start end width))

(defn nearest-enemy-distance [{:keys [world/grid]} entity]
  (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                (entity/enemy entity)))

(defn nearest-enemy [{:keys [world/grid]} entity]
  (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                       (entity/enemy entity)))

(defn line-of-sight? [{:keys [world/raycaster]}
                      source
                      target]
  (assert raycaster)
  (not (raycaster/blocked? raycaster
                           (entity/position source)
                           (entity/position target))))

(defn creatures-in-los-of
  [{:keys [world/active-entities]
    :as world}
   entity]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(line-of-sight? world entity @%))
       (remove #(:entity/player? @%))))

(defn potential-field-find-direction [{:keys [world/grid]} eid]
  (potential-fields.movement/find-direction grid eid))

(defn remove-entity! [world eid]
  (context-entity-remove! world eid)
  (mapcat (fn [[k v]]
            (when-let [destroy! (:destroy! (k entity-components))]
              (destroy! v eid world)))
          @eid))

(defn cache-active-entities [world entity]
  (assoc world :world/active-entities
         (content-grid/active-entities (:world/content-grid world)
                                       entity)))
(defn tick-potential-fields!
  [{:keys [world/factions-iterations
           world/potential-field-cache
           world/grid
           world/active-entities]}]
  (doseq [[faction max-iterations] factions-iterations]
    (potential-fields.update/tick! potential-field-cache
                                   grid
                                   faction
                                   active-entities
                                   max-iterations)))
