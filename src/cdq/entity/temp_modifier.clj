(ns cdq.entity.temp-modifier
  (:require [cdq.entity :as entity]
            [gdl.context.timer :as timer]))

(defn tick [[k {:keys [modifiers counter]}] eid c]
  (when (timer/stopped? c counter)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))
