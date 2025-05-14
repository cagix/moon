(ns cdq.tx.event
  (:require [cdq.tx :as tx]))

(defn do!
  ([eid event]
   (tx/send-event! eid event))
  ([eid event params]
   (tx/send-event! eid event params)))
