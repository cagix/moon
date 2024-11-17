(ns moon.entity.player.moving
  (:require [moon.controls :as controls]
            [moon.entity.fsm :as fsm]
            [moon.entity.stat :as stat]))

(defn ->v [eid movement-vector]
  {:eid eid
   :movement-vector movement-vector})

(defn cursor [_]
  :cursors/walking)

(defn pause-game? [_]
  false)

(defn enter [{:keys [eid movement-vector]}]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (stat/value @eid :entity/movement-speed)}))

(defn exit [{:keys [eid]}]
  (swap! eid dissoc :entity/movement))

(defn tick [{:keys [movement-vector]} eid]
  (if-let [movement-vector (controls/movement-vector)]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (stat/value @eid :entity/movement-speed)})
    (fsm/event eid :no-movement-input)))
