(ns cdq.tx.player-remove-item
  (:require [cdq.stage :as stage]))

(defn do! [[_ cell] {:keys [ctx/stage]}]
  (stage/remove-item! stage cell)
  nil)
