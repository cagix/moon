(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.ui.stage :as stage]))

(defn do! [_ ctx]
  (-> ctx
      :ctx/stage
      stage/toggle-inventory-visible!)
  nil)
