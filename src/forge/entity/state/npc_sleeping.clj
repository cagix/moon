(ns forge.entity.state.npc-sleeping
  (:require [clojure.utils :refer [defmethods]]
            [forge.entity :refer [->v tick render-above]]
            [forge.entity.body :refer [e-tile]]
            [forge.entity.faction :as faction]
            [forge.entity.fsm :refer [send-event]]
            [forge.entity.stat :as stat]
            [forge.entity.state :refer [exit]]
            [forge.entity.string-effect :as string-effect]
            [forge.graphics :refer [draw-text]]
            [forge.world :refer [delayed-alert]]
            [forge.world.grid :refer [world-grid nearest-entity-distance]]))

(defmethods :npc-sleeping
  (->v [[_ eid]]
    {:eid eid})

  (exit [[_ {:keys [eid]}]]
    (delayed-alert (:position       @eid)
                   (:entity/faction @eid)
                   0.2)
    (swap! eid string-effect/add "[WHITE]!"))

  (tick [_ eid]
    (let [entity @eid
          cell (get world-grid (e-tile entity))] ; pattern!
      (when-let [distance (nearest-entity-distance @cell (faction/enemy entity))]
        (when (<= distance (stat/->value entity :entity/aggro-range))
          (send-event eid :alert)))))

  (render-above [_ entity]
    (let [[x y] (:position entity)]
      (draw-text {:text "zzz"
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))
