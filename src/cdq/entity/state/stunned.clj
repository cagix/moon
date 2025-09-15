(ns cdq.entity.state.stunned
  (:require [cdq.timer :as timer]))

(def ^:private stunned-circle-width 0.5)
(def ^:private stunned-circle-color [1 1 1 0.6])

(defn create [_eid duration {:keys [ctx/world]}]
  {:counter (timer/create (:world/elapsed-time world) duration)})

(defn draw [_ {:keys [entity/body]} _ctx]
  [[:draw/circle
    (:body/position body)
    stunned-circle-width
    stunned-circle-color]])

(defn tick! [{:keys [counter]} eid {:keys [ctx/world]}]
  (when (timer/stopped? (:world/elapsed-time world) counter)
    [[:tx/event eid :effect-wears-off]]))
