(ns ^:no-doc forge.entity.temp-modifier
  (:require [forge.graphics :refer [draw-filled-circle]]
            [forge.entity.components :as entity]
            [forge.world :refer [stopped?]]))

(defn tick [{:keys [modifiers counter]} eid]
  (when (stopped? counter)
    (swap! eid dissoc *k*)
    (swap! eid entity/remove-mods modifiers)))

; TODO draw opacity as of counter ratio?
(defn render-above [_ entity]
  (draw-filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4]))
