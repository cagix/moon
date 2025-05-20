(ns cdq.tx.toggle-inventory-visible
  (:require [gdl.ui :as ui]))

(defn do! [{:keys [ctx/stage]}]
  (-> stage
      :windows
      :inventory-window
      ui/toggle-visible!))
