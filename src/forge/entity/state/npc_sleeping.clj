(ns forge.entity.state.npc-sleeping
  (:require [forge.entity.body :refer [e-tile]]
            [forge.entity.faction :as faction]
            [forge.entity.fsm :refer [send-event]]
            [forge.entity.stat :as stat]
            [forge.entity.string-effect :as string-effect]
            [forge.graphics :refer [draw-text]]
            [forge.world :refer [delayed-alert]]
            [forge.world.grid :refer [world-grid nearest-entity-distance]]))

(defn ->v [[_ eid]]
  {:eid eid})

(defn exit [[_ {:keys [eid]}]]
  (delayed-alert (:position       @eid)
                 (:entity/faction @eid)
                 0.2)
  (swap! eid string-effect/add "[WHITE]!"))

(defn tick [_ eid]
  (let [entity @eid
        cell (get world-grid (e-tile entity))] ; pattern!
    (when-let [distance (nearest-entity-distance @cell (faction/enemy entity))]
      (when (<= distance (stat/->value entity :entity/aggro-range))
        (send-event eid :alert)))))

(defn render-above [_ entity]
  (let [[x y] (:position entity)]
    (draw-text {:text "zzz"
                :x x
                :y (+ y (:half-height entity))
                :up? true})))
