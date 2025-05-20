(ns cdq.application.render.remove-destroyed-entities
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.entity :as entity]
            [cdq.grid :as grid]))

(defn do! []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @ctx/entity-ids))]
    (let [id (:entity/id @eid)]
      (assert (contains? @ctx/entity-ids id))
      (swap! ctx/entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (grid/remove-entity! eid)
    (doseq [component @eid]
      (ctx/handle-txs! (entity/destroy! component
                                        eid
                                        (ctx/make-map))))))
