(ns ^:no-doc anvil.entity.temp-modifier
  (:require [anvil.entity :as entity]
            [cdq.context :refer [finished-ratio stopped?]]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.utils :refer [readable-number]]
            [gdl.context :as c]))

(defcomponent :entity/temp-modifier
  (component/segment [[_ {:keys [counter]}] c]
    (str "Spiderweb - remaining: " (readable-number (finished-ratio c counter)) "/1"))

  (component/tick [[k {:keys [modifiers counter]}] eid c]
    (when (stopped? c counter)
      (swap! eid dissoc k)
      (swap! eid entity/mod-remove modifiers)))

  ; TODO draw opacity as of counter ratio?
  (component/render-above [_ entity c]
    (c/filled-circle c (:position entity) 0.5 [0.5 0.5 0.5 0.4])))
