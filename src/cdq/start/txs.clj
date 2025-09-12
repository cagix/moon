(ns cdq.start.txs
  (:require [cdq.fn-map :as fn-map]))

(defn do! [ctx]
  (update ctx :ctx/txs-fn-map fn-map/create))
