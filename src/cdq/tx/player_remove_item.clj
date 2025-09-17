(ns cdq.tx.player-remove-item
  (:require [cdq.stage :as stage]))

(defn do!
  [{:keys [ctx/stage]}
   cell]
  (stage/remove-item! stage cell)
  nil)
