(ns cdq.tx.state-exit
  (:require [cdq.entity.state :as state]))

(defn do! [[_ eid [state-k state-v]] ctx]
  (when-let [f (state-k state/->exit)]
    (f state-v eid ctx)))
