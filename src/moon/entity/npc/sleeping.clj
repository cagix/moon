(ns ^:no-doc moon.entity.npc.sleeping
  (:require [moon.component :refer [defc] :as component]
            [moon.graphics :as g]
            [moon.world :as world :refer [stopped? timer]]
            [moon.entity :as entity]))

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       world/circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defc :entity/alert-friendlies-after-duration
  {:let {:keys [counter faction]}}
  (entity/tick [_ eid]
    (when (stopped? counter)
      (cons [:e/destroy eid]
            (for [friendly-eid (friendlies-in-radius (:position @eid) faction)]
              [:tx/event friendly-eid :alert])))))

(defc :tx/shout
  (component/handle [[_ position faction delay-seconds]]
    [[:e/create
      position
      entity/effect-body-props
      {:entity/alert-friendlies-after-duration
       {:counter (timer delay-seconds)
        :faction faction}}]]))

(defc :npc-sleeping
  {:let {:keys [eid]}}
  (entity/->v [[_ eid]]
    {:eid eid})

  (entity/exit [_]
    [[:tx/add-text-effect eid "[WHITE]!"]
     [:tx/shout (:position @eid) (:entity/faction @eid) 0.2]])

  (entity/tick [_ eid]
    (let [entity @eid
          cell (world/grid (entity/tile entity))] ; pattern!
      (when-let [distance (world/nearest-entity-distance @cell (entity/enemy entity))]
        (when (<= distance (entity/stat entity :stats/aggro-range))
          [[:tx/event eid :alert]]))))

  (entity/render-above [_ entity]
    (let [[x y] (:position entity)]
      (g/draw-text {:text "zzz"
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}))))
