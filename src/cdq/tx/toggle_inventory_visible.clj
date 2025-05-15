(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.ctx :as ctx]
            [cdq.stage :as stage]))

(defn do! []
  (stage/toggle-inventory-visible! ctx/stage))
