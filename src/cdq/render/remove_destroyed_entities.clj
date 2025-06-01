(ns cdq.render.remove-destroyed-entities
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.world :as world]))

; do not pause as pickup item should be destroyed
(defn do! [{:keys [ctx/entity-ids]
            :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (world/context-entity-remove! ctx eid)
    (doseq [component @eid]
      (g/handle-txs! ctx (entity/destroy! component eid ctx))))
  ctx)
