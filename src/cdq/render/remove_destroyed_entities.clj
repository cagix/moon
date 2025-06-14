(ns cdq.render.remove-destroyed-entities
  (:require [cdq.world :as world]))

; do not pause as pickup item should be destroyed
(defn do! [{:keys [ctx/entity-ids]
            :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (world/context-entity-remove! ctx eid)
    (doseq [component @eid]
      (world/handle-txs! ctx (world/component-destroy! ctx component eid))))
  ctx)
