(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.ctx :as ctx]
            [cdq.impl.stage :as stage]))

(defn do! []
  (-> ctx/stage :windows :inventory-window stage/toggle-visible!))
