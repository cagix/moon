(ns cdq.entity.state.npc-sleeping
  (:require [cdq.world.entity :as entity]
            [cdq.world :as w]))

(defn tick! [_ eid world]
  (let [entity @eid]
    (when-let [distance (w/nearest-enemy-distance world entity)]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        [[:tx/event eid :alert]]))))

(defn exit [_ eid _ctx]
  [[:tx/spawn-alert (entity/position @eid) (entity/faction @eid) 0.2]
   [:tx/add-text-effect eid "[WHITE]!" 1]])
