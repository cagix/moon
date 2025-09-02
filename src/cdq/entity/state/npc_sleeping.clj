(ns cdq.entity.state.npc-sleeping
  (:require [cdq.world.entity :as entity]
            [cdq.world.entity.stats :as modifiers]
            [cdq.world.grid :as grid]))

(defn tick! [_ eid {:keys [world/grid]}]
  (let [entity @eid]
    (when-let [distance (grid/nearest-enemy-distance grid entity)]
      (when (<= distance (modifiers/get-stat-value (:creature/stats entity) :entity/aggro-range))
        [[:tx/event eid :alert]]))))

(defn exit [_ eid _ctx]
  [[:tx/spawn-alert (entity/position @eid) (:entity/faction @eid) 0.2]
   [:tx/add-text-effect eid "[WHITE]!" 1]])
