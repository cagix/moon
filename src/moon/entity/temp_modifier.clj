(ns moon.entity.temp-modifier
  (:require [gdl.graphics.shape-drawer :as sd]
            [gdl.utils :refer [readable-number]]
            [moon.component :as component]
            [moon.entity :as entity]
            [moon.world.time :refer [stopped? finished-ratio]]))

(defmethods :entity/temp-modifier
  {:let {:keys [counter modifiers]}}
  (component/info [_]
    (str "[LIGHT_GRAY]Spiderweb - remaining: " (readable-number (finished-ratio counter)) "/1[]"))

  (entity/tick [[k _] eid]
    (when (stopped? counter)
      [[:e/dissoc eid k]
       [:entity/modifiers eid :remove modifiers]]))

  (entity/render-above [_ entity]
    (sd/filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4])))
