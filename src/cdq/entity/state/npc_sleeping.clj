(ns cdq.entity.state.npc-sleeping
  (:require [cdq.stats :as modifiers]
            [cdq.grid :as grid]))

(defn tick! [_ eid {:keys [ctx/grid]}]
  (let [entity @eid]
    (when-let [distance (grid/nearest-enemy-distance grid entity)]
      (when (<= distance (modifiers/get-stat-value (:creature/stats entity) :entity/aggro-range))
        [[:tx/event eid :alert]]))))
