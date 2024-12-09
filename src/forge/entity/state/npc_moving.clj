(ns forge.entity.state.npc-moving
  (:require [anvil.entity :refer [send-event stat-value]]
            [anvil.world :refer [timer stopped?]]))

; npc moving is basically a performance optimization so npcs do not have to check
; usable skills every frame
; also prevents fast twitching around changing directions every frame

(defn ->v [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer (* (stat-value @eid :entity/reaction-time) 0.016))})

(defn enter [[_ {:keys [eid movement-vector]}]]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (or (stat-value @eid :entity/movement-speed) 0)}))

(defn exit [[_ {:keys [eid]}]]
  (swap! eid dissoc :entity/movement))

(defn tick [[_ {:keys [counter]}] eid]
  (when (stopped? counter)
    (send-event eid :timer-finished)))
