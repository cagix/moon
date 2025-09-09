(ns cdq.tx.state-enter
  (:require [cdq.entity.state :as state]))

(defn do! [[_ eid [state-k state-v]] _ctx]
  (when-let [f (state-k state/state->enter)]
    (f state-v eid)))
