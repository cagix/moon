(ns cdq.tx.state-exit
  (:require [cdq.ctx :as ctx]))

(defn do! [[_ eid [state-k state-v]]
           {:keys [ctx/world] :as ctx}]
  (ctx/handle-txs! ctx
                   (when-let [f (state-k (:state->exit (:world/entity-states world)))]
                     (f state-v eid ctx))))
