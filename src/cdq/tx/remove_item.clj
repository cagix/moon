(ns cdq.tx.remove-item
  (:require [cdq.ui :as ui]))

(defn do!
  [{:keys [ctx/stage] :as ctx} eid cell]
  (when (:entity/player? @eid)
    (ui/remove-item! stage cell))
  ctx)
