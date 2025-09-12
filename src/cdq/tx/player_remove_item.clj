(ns cdq.tx.player-remove-item
  (:require [cdq.ctx.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]}
   cell]
  (stage/remove-item! stage cell)
  nil)
