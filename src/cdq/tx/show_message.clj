(ns cdq.tx.show-message
  (:require [cdq.ui :as ui]))

(defn do!
  [{:keys [ctx/stage] :as ctx} message]
  (ui/show-text-message! stage message)
  ctx)
