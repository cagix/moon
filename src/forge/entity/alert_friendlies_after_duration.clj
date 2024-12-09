(ns forge.entity.alert-friendlies-after-duration
  (:require [anvil.entity :refer [send-event]]
            [forge.world.grid :refer [circle->entities]]
            [forge.world.time :refer [stopped?]]))

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defn tick [[_ {:keys [counter faction]}] eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius (:position @eid) faction)]
      (send-event friendly-eid :alert))))
