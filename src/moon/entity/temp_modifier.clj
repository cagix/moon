(ns moon.entity.temp-modifier
  (:require [moon.component :refer [defc] :as component]
            [moon.graphics :as g]
            [gdl.utils :refer [readable-number]]
            [moon.world :refer [stopped? finished-ratio]]
            [moon.entity :as entity]))

(defc :entity/temp-modifier
  {:let {:keys [counter modifiers]}}
  (component/info [_]
    (str "[LIGHT_GRAY]Spiderweb - remaining: " (readable-number (finished-ratio counter)) "/1[]"))

  (entity/tick [[k _] eid]
    (when (stopped? counter)
      [[:e/dissoc eid k]
       [:entity/modifiers eid :remove modifiers]]))

  (entity/render-above [_ entity]
    (g/draw-filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4])))
