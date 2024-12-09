(ns forge.entity.state.player-moving
  (:require [forge.controls :as controls]
            [forge.entity.fsm :refer [send-event]]
            [forge.entity.stat :as stat]
            [forge.world.time :refer [timer stopped?]]))

(defn ->v [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector})

(defn cursor [_]
  :cursors/walking)

(defn pause-game? [_]
  false)

(defn enter [[_ {:keys [eid movement-vector]}]]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (stat/->value @eid :entity/movement-speed)}))

(defn exit [[_ {:keys [eid]}]]
  (swap! eid dissoc :entity/movement))

(defn tick [[_ {:keys [movement-vector]}] eid]
  (if-let [movement-vector (controls/movement-vector)]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (stat/->value @eid :entity/movement-speed)})
    (send-event eid :no-movement-input)))
