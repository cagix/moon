(ns cdq.entity.string-effect
  (:require [gdl.context.timer :as timer]))

(defn tick [[k {:keys [counter]}] eid c]
  (when (timer/stopped? c counter)
    (swap! eid dissoc k)))
