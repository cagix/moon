(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.ctx :as ctx]
            [gdl.ui :as ui]))

(defn do! []
  (-> ctx/stage
      :windows
      :inventory-window
      ui/toggle-visible!))
