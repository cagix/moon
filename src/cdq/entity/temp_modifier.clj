(ns cdq.entity.temp-modifier
  (:require [cdq.entity :as entity]
            [cdq.context :refer [stopped? finished-ratio]]
            [gdl.utils :refer [readable-number]]
            [gdl.context :as c]))

(defn info [[_ {:keys [counter]}] c]
  (str "Spiderweb - remaining: " (readable-number (finished-ratio c counter)) "/1"))

(defn tick [[k {:keys [modifiers counter]}] eid c]
  (when (stopped? c counter)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

; TODO draw opacity as of counter ratio?
(defn render-above [_ entity c]
  (c/filled-circle c (:position entity) 0.5 [0.5 0.5 0.5 0.4]))
