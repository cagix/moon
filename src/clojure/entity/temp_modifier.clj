(ns clojure.entity.temp-modifier
  (:require [clojure.entity :as entity]
            [clojure.timer :as timer]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :entity/temp-modifier
  (entity/tick! [[k {:keys [modifiers counter]}]
                 eid
                 {:keys [ctx/elapsed-time]}]
    (when (timer/stopped? elapsed-time counter)
      [[:tx/dissoc eid k]
       [:tx/mod-remove eid modifiers]]))

  ; TODO draw opacity as of counter ratio?
  (entity/render-above! [_ entity _ctx]
    [[:draw/filled-circle (entity/position entity) 0.5 [0.5 0.5 0.5 0.4]]]))
