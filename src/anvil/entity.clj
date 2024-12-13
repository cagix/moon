(ns anvil.entity
  (:require [anvil.entity.fsm :as fsm]))

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))

(defn state-k [entity]
  (fsm/state-k entity))

(defn state-obj [entity]
  (fsm/state-obj entity))

(defn event
  ([eid event]
   (fsm/send-event! eid event nil))
  ([eid event params]
   (fsm/send-event! eid event params)))
