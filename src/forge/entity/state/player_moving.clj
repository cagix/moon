(ns forge.entity.state.player-moving
  (:require [anvil.entity :refer [send-event stat-value]]
            [anvil.world :refer [timer stopped?]]
            [forge.controls :as controls]))

(defn ->v [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector})

(defn cursor [_]
  :cursors/walking)

(defn pause-game? [_]
  false)

(defn enter [[_ {:keys [eid movement-vector]}]]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (stat-value @eid :entity/movement-speed)}))

(defn exit [[_ {:keys [eid]}]]
  (swap! eid dissoc :entity/movement))

(defn tick [[_ {:keys [movement-vector]}] eid]
  (if-let [movement-vector (controls/movement-vector)]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (stat-value @eid :entity/movement-speed)})
    (send-event eid :no-movement-input)))
