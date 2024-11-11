(ns moon.entity.npc.sleeping
  (:require [gdl.graphics.text :as text]
            [moon.body :as body]
            [moon.entity.faction :as faction]
            [moon.entity.fsm :as fsm]
            [moon.entity.stat :as stat]
            [moon.entity.string-effect :as string-effect]
            [moon.world.entities :as entities]
            [moon.world.grid :as grid]))

(defn ->v [eid]
  {:eid eid})

(defn exit [{:keys [eid]}]
  (entities/shout (:position @eid) (:entity/faction @eid) 0.2)
  (swap! eid string-effect/add "[WHITE]!"))

(defn tick [_ eid]
  (let [entity @eid
        cell (grid/cell (body/tile entity))] ; pattern!
    (when-let [distance (grid/nearest-entity-distance @cell (faction/enemy entity))]
      (when (<= distance (stat/value entity :entity/aggro-range))
        (fsm/event eid :alert)))))

(defn render-above [_ entity]
  (let [[x y] (:position entity)]
    (text/draw {:text "zzz"
                :x x
                :y (+ y (:half-height entity))
                :up? true})))
