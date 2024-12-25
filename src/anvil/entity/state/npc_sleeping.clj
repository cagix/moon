(ns ^:no-doc anvil.entity.state.npc-sleeping
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.world :as world :refer [add-text-effect]]
            [gdl.context :as c]))

(defmethods :npc-sleeping
  (component/->v [[_ eid]]
    {:eid eid})

  (component/exit [[_ {:keys [eid]}] c]
    (world/delayed-alert c
                         (:position       @eid)
                         (:entity/faction @eid)
                         0.2)
    (swap! eid add-text-effect "[WHITE]!"))

  (component/tick [_ eid c]
    (let [entity @eid
          cell (world/grid (entity/tile entity))] ; pattern!
      (when-let [distance (world/nearest-entity-distance @cell (entity/enemy entity))]
        (when (<= distance (entity/stat entity :entity/aggro-range))
          (entity/event c eid :alert)))))

  (component/render-above [_ entity c]
    (let [[x y] (:position entity)]
      (c/draw-text c
                   {:text "zzz"
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}))))
