(ns cdq.tx.show-message
  (:require [cdq.ui :as ui]))

(defn do!
  [{:keys [ctx/stage]} message]
  (ui/show-text-message! stage message)
  nil)
