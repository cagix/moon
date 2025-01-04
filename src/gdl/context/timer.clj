(ns gdl.context.timer
  (:require [gdl.timer :as timer]))

(defn create [{::keys [elapsed-time]} duration]
  (timer/create elapsed-time duration))

(defn stopped? [{::keys [elapsed-time]} timer]
  (timer/stopped? timer elapsed-time))

(defn reset [{::keys [elapsed-time]} timer]
  (timer/reset timer elapsed-time))

(defn ratio [{::keys [elapsed-time]} timer]
  (timer/ratio timer elapsed-time))
