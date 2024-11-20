(ns moon.entity.temp-modifier
  (:require [moon.system :refer [*k*]]
            [gdl.utils :refer [readable-number]]
            [moon.app :refer [draw-filled-circle]]
            [moon.entity :as entity]
            [moon.world :refer [stopped? finished-ratio]]))

(defn info [{:keys [counter]}]
  (str "[LIGHT_GRAY]Spiderweb - remaining: " (readable-number (finished-ratio counter)) "/1[]"))

(defn tick [{:keys [modifiers counter]} eid]
  (when (stopped? counter)
    (swap! eid dissoc *k*)
    (swap! eid entity/remove-mods modifiers)))

; TODO draw opacity as of counter ratio?
(defn render-above [_ entity]
  (draw-filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4]))
