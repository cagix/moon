(ns cdq.game.remove-destroyed-entities
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.utils :as utils]
            [cdq.world :as world]))

(defn do! []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @(:entity-ids ctx/world)))]
    (world/remove-entity! ctx/world eid)
    (doseq [component @eid]
      (utils/handle-txs! (entity/destroy! component eid)))))
