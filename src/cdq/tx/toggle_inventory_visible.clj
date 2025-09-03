(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.render.handle-key-input :refer [toggle-inventory-visible!]]))

(defn do! [_ ctx]
  (-> ctx
      :ctx/stage
      toggle-inventory-visible!)
  nil)
