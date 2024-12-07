(ns forge.entity.temp-modifier
  (:require [clojure.utils :refer [defmethods]]
            [forge.app.shape-drawer :as sd]
            [forge.entity :refer [tick render-above]]
            [forge.entity.modifiers :as mods]
            [forge.world.time :refer [stopped?]]))

(defmethods :entity/temp-modifier
  (tick [[k {:keys [modifiers counter]}] eid]
    (when (stopped? counter)
      (swap! eid dissoc k)
      (swap! eid mods/remove modifiers)))

  ; TODO draw opacity as of counter ratio?
  (render-above [_ entity]
    (sd/filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4])))
