(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.c :as c]
            [gdl.ui :as ui]))

(defn do! [ctx]
  (-> (c/get-actor ctx :windows)
      :inventory-window
      ui/toggle-visible!))
