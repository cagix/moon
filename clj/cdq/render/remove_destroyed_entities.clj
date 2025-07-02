(ns cdq.render.remove-destroyed-entities
  (:require [cdq.ctx :as ctx]
            [cdq.w :as w]))

(defn do! [{:keys [ctx/world]
            :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @(:world/entity-ids world)))]
    (ctx/handle-txs! ctx (w/remove-entity! world eid)))
  ctx)
