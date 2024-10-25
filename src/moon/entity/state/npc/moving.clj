(ns ^:no-doc moon.entity.state.npc.moving
  (:require [moon.component :refer [defc]]
            [moon.world :refer [timer stopped?]]
            [moon.entity :as entity]
            [moon.entity.state :as state]))

; npc moving is basically a performance optimization so npcs do not have to check
; pathfindinusable skills every frame
; also prevents fast twitching around changing directions every frame
(defc :npc-moving
  {:let {:keys [eid movement-vector counter]}}
  (entity/->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector
     :counter (timer (* (entity/stat @eid :stats/reaction-time) 0.016))})

  (state/enter [_]
    [[:tx/set-movement eid {:direction movement-vector
                            :speed (or (entity/stat @eid :stats/movement-speed) 0)}]])

  (state/exit [_]
    [[:tx/set-movement eid nil]])

  (entity/tick [_ eid]
    (when (stopped? counter)
      [[:tx/event eid :timer-finished]])))
