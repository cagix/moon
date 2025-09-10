(ns cdq.entity.state.stunned
  (:require [cdq.timer :as timer]))

(def ^:private stunned-circle-width 0.5)
(def ^:private stunned-circle-color [1 1 1 0.6])

(defn create [_eid duration {:keys [ctx/elapsed-time]}]
  {:counter (timer/create elapsed-time duration)})

(defn draw [_ {:keys [entity/body]} _ctx]
  [[:draw/circle
    (:body/position body)
    stunned-circle-width
    stunned-circle-color]])

(defn tick! [{:keys [counter]} eid {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/event eid :effect-wears-off]]))
