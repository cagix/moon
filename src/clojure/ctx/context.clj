(ns clojure.ctx.context
  (:require [clojure.content-grid :as content-grid]
            [clojure.entity :as entity]
            [clojure.grid :as grid]))

(defn context-entity-add! [{:keys [ctx/entity-ids
                                   ctx/content-grid
                                   ctx/grid]}
                           eid]
  (let [id (entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))
  (content-grid/add-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (grid/add-entity! grid eid))

(defn context-entity-remove! [{:keys [ctx/entity-ids
                                      ctx/grid]}
                              eid]
  (let [id (entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))
  (content-grid/remove-entity! eid)
  (grid/remove-entity! grid eid))

(defn context-entity-moved! [{:keys [ctx/content-grid
                                     ctx/grid]}
                             eid]
  (content-grid/position-changed! content-grid eid)
  (grid/position-changed! grid eid))
