(ns moon.entity.npc.sleeping
  (:require [gdl.graphics.text :as text]
            [moon.component :as component]
            [moon.body :as body]
            [moon.entity :as entity]
            [moon.faction :as faction]
            [moon.world.grid :as grid]
            [moon.world.time :refer [stopped? timer]]))

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       grid/circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defmethods :entity/alert-friendlies-after-duration
  (entity/tick [[_ {:keys [counter faction]}] eid]
    (when (stopped? counter)
      (cons [:e/destroy eid]
            (for [friendly-eid (friendlies-in-radius (:position @eid) faction)]
              [:entity/fsm friendly-eid :alert])))))

(defmethods :tx/shout
  (component/handle [[_ position faction delay-seconds]]
    [[:e/create
      position
      body/effect-body-props
      {:entity/alert-friendlies-after-duration
       {:counter (timer delay-seconds)
        :faction faction}}]]))

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
