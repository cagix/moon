(ns cdq.tx.toggle-inventory-visible
  (:require [gdl.c :as c]
            [gdl.ui :as ui]))

(defn do! [ctx]
  (-> (c/get-actor ctx :windows)
      :inventory-window
      ui/toggle-visible!))
