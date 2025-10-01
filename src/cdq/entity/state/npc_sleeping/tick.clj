(ns cdq.entity.state.npc-sleeping.tick
  (:require [cdq.entity.stats :as stats]
            [cdq.world.grid :as grid]))

(defn txs [_ eid {:keys [world/grid]}]
  (let [entity @eid]
    (when-let [distance (grid/nearest-enemy-distance grid entity)]
      (when (<= distance (stats/get-stat-value (:entity/stats entity) :stats/aggro-range))
        [[:tx/event eid :alert]]))))
