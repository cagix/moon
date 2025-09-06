(ns cdq.entity.state.npc-moving
  (:require [cdq.timer :as timer]))

(defn tick! [{:keys [timer]} eid {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time timer)
    [[:tx/event eid :timer-finished]]))
