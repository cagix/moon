(ns cdq.entity.state.npc-sleeping
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.state :as state]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :npc-sleeping
  (entity/tick! [_ eid ctx]
    (let [entity @eid]
      (when-let [distance (g/nearest-enemy-distance ctx entity)]
        (when (<= distance (entity/stat entity :entity/aggro-range))
          [[:tx/event eid :alert]]))))

  (state/exit! [_ eid _ctx]
    [[:tx/spawn-alert (:position @eid) (:entity/faction @eid) 0.2]
     [:tx/add-text-effect eid "[WHITE]!"]])

  (entity/render-above! [_ entity _ctx]
    (let [[x y] (:position entity)]
      [[:draw/text {:text "zzz"
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}]])))
