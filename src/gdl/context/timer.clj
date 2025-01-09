(ns gdl.context.timer
  (:require [cdq.timer :as timer]))

(defn create   [{:keys [gdl.context/elapsed-time]} duration] (timer/create elapsed-time duration))
(defn stopped? [{:keys [gdl.context/elapsed-time]} timer]    (timer/stopped? timer elapsed-time))
(defn reset    [{:keys [gdl.context/elapsed-time]} timer]    (timer/reset    timer elapsed-time))
(defn ratio    [{:keys [gdl.context/elapsed-time]} timer]    (timer/ratio    timer elapsed-time))
