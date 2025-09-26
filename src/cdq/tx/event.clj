(ns cdq.tx.event
  (:require [cdq.world :as world]))

(defn do! [{:keys [ctx/world]} & params]
  (apply world/handle-event world params))
