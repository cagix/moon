(ns cdq.entity.string-effect
  (:require [cdq.timer :as timer]))

(defn tick! [[k {:keys [counter]}] eid {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc eid k]]))
