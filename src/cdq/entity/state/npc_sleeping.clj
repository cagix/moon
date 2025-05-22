(ns cdq.entity.state.npc-sleeping
  (:require [cdq.cell :as cell]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :npc-sleeping
  (entity/tick! [_ eid {:keys [ctx/grid]}]
    (let [entity @eid
          cell (grid (mapv int (:position entity)))]
      (when-let [distance (cell/nearest-entity-distance @cell (entity/enemy entity))]
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
