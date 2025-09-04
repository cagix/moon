(ns cdq.render.remove-destroyed-entities
  (:require [cdq.ctx :as ctx]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]))

(declare entity-components)

(defn do!
  [{:keys [ctx/entity-ids
           ctx/world]
    :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (let [id (:entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (grid/remove-entity! (:world/grid world) eid)
    (ctx/handle-txs! ctx (mapcat (fn [[k v]]
                                   (when-let [destroy! (:destroy! (k entity-components))]
                                     (destroy! v eid world)))
                                 @eid))))
