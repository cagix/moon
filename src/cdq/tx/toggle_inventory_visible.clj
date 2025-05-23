(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.g :as g]
            [gdl.ui :as ui]))

(defn do! [ctx]
  (-> (g/get-actor ctx :windows)
      :inventory-window
      ui/toggle-visible!))
