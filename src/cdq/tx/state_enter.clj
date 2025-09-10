(ns cdq.tx.state-enter
  (:require [cdq.entity.state :as state]))

(defn do! [_ctx eid [state-k state-v]]
  (when-let [f (state-k state/state->enter)]
    (f state-v eid)))
