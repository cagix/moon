(ns cdq.world.tx.toggle-inventory-visible
  (:require [cdq.ui :as ui]))

(defn do! [{:keys [ctx/stage]}]
  (ui/toggle-inventory-visible! stage)
  nil)
