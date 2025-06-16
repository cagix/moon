(ns cdq.render.remove-destroyed-entities
  (:require [cdq.ctx :as ctx]
            [cdq.world :as world]))

(defn do! [{:keys [ctx/world]
            :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @(:world/entity-ids world)))]
    (world/context-entity-remove! world eid)
    (doseq [component @eid]
      (ctx/handle-txs! ctx (world/component-destroy! world component eid))))
  ctx)
