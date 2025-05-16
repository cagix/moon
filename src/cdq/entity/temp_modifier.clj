(ns cdq.entity.temp-modifier
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.timer :as timer]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/temp-modifier
  (entity/tick! [[k {:keys [modifiers counter]}] eid]
    (when (timer/stopped? ctx/elapsed-time counter)
      [[:tx/dissoc eid k]
       [:tx/mod-remove eid modifiers]]))

  ; TODO draw opacity as of counter ratio?
  (entity/render-above! [_ entity]
    (draw/filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4])))
