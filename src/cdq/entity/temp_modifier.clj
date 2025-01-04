(ns cdq.entity.temp-modifier
  (:require [cdq.entity :as entity]
            [gdl.context :as c]
            [gdl.context.timer :as timer]))

(defn tick [[k {:keys [modifiers counter]}] eid c]
  (when (timer/stopped? c counter)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

; TODO draw opacity as of counter ratio?
(defn render-above [_ entity c]
  (c/filled-circle c (:position entity) 0.5 [0.5 0.5 0.5 0.4]))
