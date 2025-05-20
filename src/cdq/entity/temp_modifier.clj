(ns cdq.entity.temp-modifier
  (:require [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.timer :as timer]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/temp-modifier
  (entity/tick! [[k {:keys [modifiers counter]}] eid {:keys [ctx/elapsed-time]}]
    (when (timer/stopped? elapsed-time counter)
      [[:tx/dissoc eid k]
       [:tx/mod-remove eid modifiers]]))

  ; TODO draw opacity as of counter ratio?
  (entity/render-above! [_ entity draw]
    (draw/filled-circle draw (:position entity) 0.5 [0.5 0.5 0.5 0.4])))
