(ns cdq.entity.state.npc-sleeping
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.utils :refer [defcomponent]]
            [cdq.world.grid.cell :as cell]))

(defcomponent :npc-sleeping
  (entity/create [[_ eid]]
    {:eid eid})

  (entity/tick! [_ eid]
    (let [entity @eid
          cell (ctx/grid (entity/tile entity))]
      (when-let [distance (cell/nearest-entity-distance @cell (entity/enemy entity))]
        (when (<= distance (entity/stat entity :entity/aggro-range))
          [[:tx/event eid :alert]]))))

  (state/exit! [[_ {:keys [eid]}]]
    [[:tx/spawn-alert (:position @eid) (:entity/faction @eid) 0.2]
     [:tx/add-text-effect eid "[WHITE]!"]])

  (entity/render-above! [_ entity]
    (let [[x y] (:position entity)]
      (draw/text {:text "zzz"
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))
