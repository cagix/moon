(ns cdq.game.remove-destroyed-entities
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.utils :as utils]
            [cdq.impl.world]))

(defn do! []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @ctx/entity-ids))]
    (cdq.impl.world/remove-entity! eid)
    (doseq [component @eid]
      (utils/handle-txs! (entity/destroy! component eid)))))
