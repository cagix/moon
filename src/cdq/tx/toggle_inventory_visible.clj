(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.ctx.stage :as stage]))

(defn do! [{:keys [ctx/stage]}]
  (stage/toggle-inventory-visible! stage)
  nil)
