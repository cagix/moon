(ns moon.entity.npc.moving
  (:require [moon.entity :as entity]
            [moon.world.time :refer [timer stopped?]]))

; npc moving is basically a performance optimization so npcs do not have to check
; pathfindinusable skills every frame
; also prevents fast twitching around changing directions every frame
(defmethods :npc-moving
  {:let {:keys [eid movement-vector counter]}}
  (entity/->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector
     :counter (timer (* (entity/stat @eid :stats/reaction-time) 0.016))})

  (entity/enter [_]
    [[:entity/movement eid {:direction movement-vector
                            :speed (or (entity/stat @eid :stats/movement-speed) 0)}]])

  (entity/exit [_]
    [[:entity/movement eid nil]])

  (entity/tick [_ eid]
    (when (stopped? counter)
      [[:entity/fsm eid :timer-finished]])))
