(ns moon.entity.npc.sleeping
  (:require [gdl.graphics.text :as text]
            [moon.body :as body]
            [moon.entity :as entity]
            [moon.entity.faction :as faction]
            [moon.world.grid :as grid]))

(defn ->v [[_ eid]]
  {:eid eid})

(defn exit [[_ {:keys [eid]}]]
  [[:entity/string-effect eid "[WHITE]!"]
   [:tx/shout (:position @eid) (:entity/faction @eid) 0.2]])

(defn tick [_ eid]
  (let [entity @eid
        cell (grid/cell (body/tile entity))] ; pattern!
    (when-let [distance (grid/nearest-entity-distance @cell (faction/enemy entity))]
      (when (<= distance (entity/stat entity :stats/aggro-range))
        [[:entity/fsm eid :alert]]))))

(defn render-above [_ entity]
  (let [[x y] (:position entity)]
    (text/draw {:text "zzz"
                :x x
                :y (+ y (:half-height entity))
                :up? true})))
