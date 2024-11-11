(ns moon.entity.npc.moving
  (:require [moon.entity.fsm :as fsm]
            [moon.entity.stat :as stat]
            [moon.world.time :refer [timer stopped?]]))

; npc moving is basically a performance optimization so npcs do not have to check
; usable skills every frame
; also prevents fast twitching around changing directions every frame

(defn ->v
  ([eid]
   (->v eid nil))
  ([eid movement-vector]
   {:eid eid
    :movement-vector movement-vector
    :counter (timer (* (stat/value @eid :entity/reaction-time) 0.016))}))

(defn enter [{:keys [eid movement-vector]}]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (or (stat/value @eid :entity/movement-speed) 0)}))

(defn exit [{:keys [eid]}]
  (swap! eid dissoc :entity/movement))

(defn tick [{:keys [counter]} eid]
  (when (stopped? counter)
    (fsm/event eid :timer-finished)))
