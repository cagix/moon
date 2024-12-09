(ns forge.entity.temp-modifier
  (:require [anvil.graphics :as g]
            [forge.entity.modifiers :as mods]
            [forge.world.time :refer [stopped?]]))

(defn tick [[k {:keys [modifiers counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)
    (swap! eid mods/remove modifiers)))

; TODO draw opacity as of counter ratio?
(defn render-above [_ entity]
  (g/filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4]))
