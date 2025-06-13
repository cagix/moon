(ns cdq.entity.state.npc-sleeping
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.utils :refer [defmethods]]))

(defn tick! [_ eid ctx]
  (let [entity @eid]
    (when-let [distance (ctx/nearest-enemy-distance ctx entity)]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        [[:tx/event eid :alert]]))))

(defmethods :npc-sleeping
  (state/exit! [_ eid _ctx]
    [[:tx/spawn-alert (entity/position @eid) (entity/faction @eid) 0.2]
     [:tx/add-text-effect eid "[WHITE]!" 1]]))
