(ns cdq.entity.temp-modifier
  (:require [anvil.entity :as entity]
            [cdq.context :refer [stopped?]]
            [gdl.context :as c]))

(defn tick [[k {:keys [modifiers counter]}] eid c]
  (when (stopped? c counter)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

; TODO draw opacity as of counter ratio?
(defn render-above [_ entity c]
  (c/filled-circle c (:position entity) 0.5 [0.5 0.5 0.5 0.4]))


