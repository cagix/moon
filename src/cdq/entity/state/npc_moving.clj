(ns cdq.entity.state.npc-moving
  (:require [cdq.stats :as stats]
            [cdq.timer :as timer]))

(def reaction-time-multiplier 0.016)

; can put the params movement-vector
; and pass with enter/exit also
; so this can be only a timer then
(defn create [eid movement-vector {:keys [ctx/elapsed-time]}]
  {:movement-vector movement-vector
   :timer (timer/create elapsed-time
                        (* (stats/get-stat-value (:creature/stats @eid) :entity/reaction-time)
                           reaction-time-multiplier))})

(defn enter [{:keys [movement-vector]} eid]
  [[:tx/set-movement eid movement-vector]])

(defn exit [_ eid _ctx]
  [[:tx/dissoc eid :entity/movement]])

(defn tick! [{:keys [timer]} eid {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time timer)
    [[:tx/event eid :timer-finished]]))
