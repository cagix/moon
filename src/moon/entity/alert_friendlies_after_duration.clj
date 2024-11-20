(ns moon.entity.alert-friendlies-after-duration
  (:require [moon.entity :as entity]
            [moon.world :as world :refer [stopped?]]))

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       world/circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defn tick [{:keys [counter faction]} eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius (:position @eid) faction)]
      (entity/event friendly-eid :alert))))

