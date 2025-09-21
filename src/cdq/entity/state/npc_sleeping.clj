(ns cdq.entity.state.npc-sleeping
  (:require [cdq.entity :as entity]
            [cdq.world.grid :as grid]
            [cdq.stats :as stats]))

(defn tick! [_ eid {:keys [ctx/world]}]
  (let [entity @eid]
    (when-let [distance (grid/nearest-enemy-distance (:world/grid world) entity)]
      (when (<= distance (stats/get-stat-value (:creature/stats entity) :entity/aggro-range))
        [[:tx/event eid :alert]]))))

(defn exit [_ eid _ctx]
  [[:tx/spawn-alert (entity/position @eid) (:entity/faction @eid) 0.2]
   [:tx/add-text-effect eid "[WHITE]!" 1]])
