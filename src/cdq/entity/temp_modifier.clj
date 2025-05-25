(ns cdq.entity.temp-modifier
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :entity/temp-modifier
  (entity/tick! [[k {:keys [modifiers counter]}] eid ctx]
    (when (g/timer-stopped? ctx counter)
      [[:tx/dissoc eid k]
       [:tx/mod-remove eid modifiers]]))

  ; TODO draw opacity as of counter ratio?
  (entity/render-above! [_ entity _ctx]
    [[:draw/filled-circle (entity/position entity) 0.5 [0.5 0.5 0.5 0.4]]]))
