(ns moon.entity.npc.moving
  (:require [moon.entity.modifiers :as mods]
            [moon.world.time :refer [timer stopped?]]))

; npc moving is basically a performance optimization so npcs do not have to check
; pathfindinusable skills every frame
; also prevents fast twitching around changing directions every frame

(defn ->v [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer (* (mods/value @eid :stats/reaction-time) 0.016))})

(defn enter [[_ {:keys [eid movement-vector]}]]
  [[:entity/movement eid {:direction movement-vector
                          :speed (or (mods/value @eid :stats/movement-speed) 0)}]])

(defn exit [[_ {:keys [eid]}]]
  [[:entity/movement eid nil]])

(defn tick [[_ {:keys [counter]}] eid]
  (when (stopped? counter)
    [[:entity/fsm eid :timer-finished]]))
