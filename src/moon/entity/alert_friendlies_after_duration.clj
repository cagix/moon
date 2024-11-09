(ns moon.entity.alert-friendlies-after-duration
  (:require [moon.world.grid :as grid]
            [moon.world.time :refer [stopped?]]))

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       grid/circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defn tick [{:keys [counter faction]} eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)
    (for [friendly-eid (friendlies-in-radius (:position @eid) faction)]
      [:entity/fsm friendly-eid :alert])))

