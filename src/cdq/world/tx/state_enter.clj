(ns cdq.world.tx.state-enter
  (:require [cdq.entity.state :as state]))

(defn do! [_ctx eid [state-k state-v]]
  (state/enter [state-k state-v] eid))
