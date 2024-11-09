(ns moon.entity.temp-modifier
  (:require [gdl.graphics.shape-drawer :as sd]
            [gdl.system :refer [*k*]]
            [gdl.utils :refer [readable-number]]
            [moon.world.time :refer [stopped? finished-ratio]]))

(defn info [{:keys [counter]}]
  (str "[LIGHT_GRAY]Spiderweb - remaining: " (readable-number (finished-ratio counter)) "/1[]"))

(defn tick [{:keys [modifiers counter]} eid]
  (when (stopped? counter)
    (swap! eid dissoc *k*)
    [[:entity/modifiers eid :remove modifiers]]))

; TODO draw opacity as of counter ratio?
(defn render-above [_ entity]
  (sd/filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4]))
