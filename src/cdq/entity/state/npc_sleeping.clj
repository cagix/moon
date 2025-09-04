(ns cdq.entity.state.npc-sleeping
  (:require [cdq.entity :as entity]
            [cdq.stats :as modifiers]
            [cdq.grid :as grid]))

(defn tick! [_ eid {:keys [ctx/grid]}]
  (let [entity @eid]
    (when-let [distance (grid/nearest-enemy-distance grid entity)]
      (when (<= distance (modifiers/get-stat-value (:creature/stats entity) :entity/aggro-range))
        [[:tx/event eid :alert]]))))

(defn exit [_ eid _ctx]
  [[:tx/spawn-alert (entity/position @eid) (:entity/faction @eid) 0.2]
   [:tx/add-text-effect eid "[WHITE]!" 1]])
