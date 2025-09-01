(ns cdq.tx.event
  (:require [cdq.entity.fsm :as fsm]))

(defn do! [[_ eid event params] {:keys [ctx/world]}]
  (fsm/event->txs world eid event params))
