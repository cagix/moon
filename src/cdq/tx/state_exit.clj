(ns cdq.tx.state-exit
  (:require [cdq.entity.state :as state]))

(defn do! [ctx eid [state-k state-v]]
  (when-let [f (state-k state/state->exit)]
    (f state-v eid ctx)))
