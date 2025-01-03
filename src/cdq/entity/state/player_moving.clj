(ns cdq.entity.state.player-moving
  (:require [anvil.controls :as controls]
            [cdq.context :as world]
            [cdq.entity :as entity]))

(defn cursor [_]
  :cursors/walking)

(defn pause-game? [_]
  false)

(defn create [[_ eid movement-vector] c]
  {:eid eid
   :movement-vector movement-vector})

(defn enter [[_ {:keys [eid movement-vector]}] c]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (entity/stat @eid :entity/movement-speed)}))

(defn exit [[_ {:keys [eid]}] c]
  (swap! eid dissoc :entity/movement))

(defn tick [[_ {:keys [movement-vector]}] eid c]
  (if-let [movement-vector (controls/movement-vector c)]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)})
    (world/send-event! c eid :no-movement-input)))
