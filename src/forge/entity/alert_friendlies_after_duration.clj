(ns forge.entity.alert-friendlies-after-duration
  (:require [anvil.fsm :as fsm]
            [anvil.grid :as grid]
            [anvil.time :refer [stopped?]]))

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       grid/circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defn tick [[_ {:keys [counter faction]}] eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius (:position @eid) faction)]
      (fsm/event friendly-eid :alert))))
