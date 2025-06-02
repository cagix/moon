(ns cdq.render.remove-destroyed-entities
  (:require [clojure.ctx :as ctx]
            [cdq.entity :as entity]))

; do not pause as pickup item should be destroyed
(defn do! [{:keys [ctx/entity-ids]
            :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (ctx/context-entity-remove! ctx eid)
    (doseq [component @eid]
      (ctx/handle-txs! ctx (entity/destroy! component eid ctx))))
  ctx)
