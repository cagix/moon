(ns cdq.world.tx.state-exit
  (:require [cdq.entity.state :as state]))

(defn do! [ctx eid [state-k state-v]]
  (state/exit [state-k state-v] eid ctx))
