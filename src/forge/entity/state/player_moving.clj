(ns forge.entity.state.player-moving
  (:require [anvil.controls :as controls]
            [anvil.fsm :as fsm]
            [anvil.stat :as stat]
            [anvil.time :refer [timer stopped?]]))

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
    (fsm/event eid :no-movement-input)))
