(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.ui :as ui]))

(defn do!
  [{:keys [ctx/stage] :as ctx}]
  (ui/toggle-inventory-visible! stage)
  ctx)
