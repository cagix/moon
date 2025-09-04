(ns cdq.render.remove-destroyed-entities
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]))

(declare entity-components)

(defn do!
  [{:keys [ctx/entity-ids
           ctx/grid]
    :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (let [id (:entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (grid/remove-entity! grid eid)
    (ctx/handle-txs! ctx (mapcat (fn [[k v]]
                                   (when-let [destroy! (:destroy! (k entity-components))]
                                     (destroy! v eid ctx)))
                                 @eid))))
