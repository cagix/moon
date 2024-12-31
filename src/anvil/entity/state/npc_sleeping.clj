(ns ^:no-doc anvil.entity.state.npc-sleeping
  (:require [anvil.entity :as entity]
            [cdq.context :as world :refer [add-text-effect]]
            [cdq.grid :as grid]
            [clojure.utils :refer [defmethods]]
            [gdl.context :as c]))

(defmethods :npc-sleeping
  (entity/->v [[_ eid] c]
    {:eid eid})

  (entity/exit [[_ {:keys [eid]}] c]
    (world/delayed-alert c
                         (:position       @eid)
                         (:entity/faction @eid)
                         0.2)
    (swap! eid add-text-effect c "[WHITE]!"))

  (entity/tick [_ eid c]
    (let [entity @eid
          cell (world/grid-cell c (entity/tile entity))] ; pattern!
      (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
        (when (<= distance (entity/stat entity :entity/aggro-range))
          (entity/event c eid :alert)))))

  (entity/render-above [_ entity c]
    (let [[x y] (:position entity)]
      (c/draw-text c
                   {:text "zzz"
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}))))
