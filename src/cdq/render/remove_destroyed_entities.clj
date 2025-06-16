(ns cdq.render.remove-destroyed-entities
  (:require [cdq.ctx :as ctx]
            [cdq.world :as world]))

(defn do! [{:keys [ctx/entity-ids]
            :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (world/context-entity-remove! ctx eid)
    (doseq [component @eid]
      (ctx/handle-txs! ctx (world/component-destroy! ctx component eid))))
  ctx)
