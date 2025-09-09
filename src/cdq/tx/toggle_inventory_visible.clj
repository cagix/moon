(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.stage :as stage]))

(defn do! [_ {:keys [ctx/stage]}]
  (stage/toggle-inventory-visible! stage)
  nil)
