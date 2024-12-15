(ns ^:no-doc anvil.entity.temp-modifier
  (:require [anvil.component :as component]
            [anvil.entity.modifiers :as mods]
            [anvil.world :refer [finished-ratio stopped?]]
            [gdl.graphics :as g]
            [gdl.utils :refer [defmethods readable-number]]))

(defmethods :entity/temp-modifier
  (component/info [[_ {:keys [counter]}]]
    (str "Spiderweb - remaining: " (readable-number (finished-ratio counter)) "/1"))

  (component/tick [[k {:keys [modifiers counter]}] eid]
    (when (stopped? counter)
      (swap! eid dissoc k)
      (swap! eid mods/remove modifiers)))

  ; TODO draw opacity as of counter ratio?
  (component/render-above [_ entity]
    (g/filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4])))
