(ns cdq.render.remove-destroyed-entities
  (:require [cdq.ctx :as ctx]
            [cdq.world :as world]))

(defn do!
  [{:keys [ctx/world]
    :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @(:world/entity-ids world)))]
    (world/context-entity-remove! world eid)
    (ctx/handle-txs! ctx (mapcat (fn [[k v]]
                                   (when-let [destroy! (:destroy! (k world/entity-components))]
                                     (destroy! v eid world)))
                                 @eid)))
  ctx)
