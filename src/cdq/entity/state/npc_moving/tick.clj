(ns cdq.entity.state.npc-moving.tick
  (:require [cdq.timer :as timer]))

(defn txs [{:keys [timer]} eid {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time timer)
    [[:tx/event eid :timer-finished]]))
