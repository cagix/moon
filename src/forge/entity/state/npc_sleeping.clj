(ns forge.entity.state.npc-sleeping
  (:require [anvil.graphics :refer [draw-text]]
            [anvil.entity :as entity :refer [send-event stat-value]]
            [forge.world :refer [delayed-alert]]
            [forge.world.grid :refer [world-grid nearest-entity-distance]]))

(defn ->v [[_ eid]]
  {:eid eid})

(defn exit [[_ {:keys [eid]}]]
  (delayed-alert (:position       @eid)
                 (:entity/faction @eid)
                 0.2)
  (swap! eid entity/add-string-effect "[WHITE]!"))

(defn tick [_ eid]
  (let [entity @eid
        cell (get world-grid (entity/tile entity))] ; pattern!
    (when-let [distance (nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (stat-value entity :entity/aggro-range))
        (send-event eid :alert)))))

(defn render-above [_ entity]
  (let [[x y] (:position entity)]
    (draw-text {:text "zzz"
                :x x
                :y (+ y (:half-height entity))
                :up? true})))
