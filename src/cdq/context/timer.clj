(ns cdq.context.timer
  (:require [cdq.timer :as timer]))

(defn create   [{:keys [clojure.context/elapsed-time]} duration] (timer/create elapsed-time duration))
(defn stopped? [{:keys [clojure.context/elapsed-time]} timer]    (timer/stopped? timer elapsed-time))
(defn reset    [{:keys [clojure.context/elapsed-time]} timer]    (timer/reset    timer elapsed-time))
(defn ratio    [{:keys [clojure.context/elapsed-time]} timer]    (timer/ratio    timer elapsed-time))
