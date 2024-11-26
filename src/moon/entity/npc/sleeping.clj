(ns ^:no-doc moon.entity.npc.sleeping
  (:require [forge.graphics :refer [draw-text]]
            [moon.entity :as entity]
            [moon.world :as world]))

(defn ->v [eid]
  {:eid eid})

(defn exit [{:keys [eid]}]
  (world/shout (:position @eid) (:entity/faction @eid) 0.2)
  (swap! eid entity/add-text-effect "[WHITE]!"))

(defn tick [_ eid]
  (let [entity @eid
        cell (world/cell (entity/tile entity))] ; pattern!
    (when-let [distance (world/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (entity/event eid :alert)))))

(defn render-above [_ entity]
  (let [[x y] (:position entity)]
    (draw-text {:text "zzz"
                :x x
                :y (+ y (:half-height entity))
                :up? true})))
