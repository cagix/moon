(ns ^:no-doc anvil.entity.state.npc-sleeping
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.world :as world :refer [add-text-effect]]
            [gdl.context :as c]
            [gdl.graphics :as g]))

(defmethods :npc-sleeping
  (component/->v [[_ eid]]
    {:eid eid})

  (component/exit [[_ {:keys [eid]}]]
    (world/delayed-alert (:position       @eid)
                         (:entity/faction @eid)
                         0.2)
    (swap! eid add-text-effect "[WHITE]!"))

  (component/tick [_ eid]
    (let [entity @eid
          cell (world/grid (entity/tile entity))] ; pattern!
      (when-let [distance (world/nearest-entity-distance @cell (entity/enemy entity))]
        (when (<= distance (entity/stat entity :entity/aggro-range))
          (entity/event eid :alert)))))

  (component/render-above [_ entity]
    (let [[x y] (:position entity)]
      (g/draw-text (c/get-ctx)
                   {:text "zzz"
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}))))
