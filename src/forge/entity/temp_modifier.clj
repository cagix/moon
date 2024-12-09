(ns forge.entity.temp-modifier
  (:require [anvil.entity :as entity]
            [anvil.graphics :as g]
            [anvil.world :refer [stopped?]]))

(defn tick [[k {:keys [modifiers counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

; TODO draw opacity as of counter ratio?
(defn render-above [_ entity]
  (g/filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4]))
