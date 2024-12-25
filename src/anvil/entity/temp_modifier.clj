(ns ^:no-doc anvil.entity.temp-modifier
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [cdq.context :refer [finished-ratio stopped?]]
            [gdl.context :as c]))

(defmethods :entity/temp-modifier
  (component/info [[_ {:keys [counter]}]]
    (str "Spiderweb - remaining: " (readable-number (finished-ratio counter)) "/1"))

  (component/tick [[k {:keys [modifiers counter]}] eid c]
    (when (stopped? counter)
      (swap! eid dissoc k)
      (swap! eid entity/mod-remove modifiers)))

  ; TODO draw opacity as of counter ratio?
  (component/render-above [_ entity c]
    (c/filled-circle c (:position entity) 0.5 [0.5 0.5 0.5 0.4])))
