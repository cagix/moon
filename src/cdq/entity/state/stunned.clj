(ns cdq.entity.state.stunned
  (:require [cdq.timer :as timer]))

(defn create [_eid duration {:keys [world/elapsed-time]}]
  {:counter (timer/create elapsed-time duration)})

(def ^:private stunned-circle-width 0.5)
(def ^:private stunned-circle-color [1 1 1 0.6])

(defn draw [_ {:keys [entity/body]} _ctx]
  [[:draw/circle
    (:body/position body)
    stunned-circle-width
    stunned-circle-color]])
