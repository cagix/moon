(ns cdq.entity.temp-modifier
  (:require [cdq.entity :as entity]
            [cdq.timer :as timer]
            [clojure.utils :as utils]))

; TODO draw opacity as of counter ratio?
(defn draw [_ entity _ctx]
  [[:draw/filled-circle (entity/position entity) 0.5 [0.5 0.5 0.5 0.4]]])

(defn tick!
  [{:keys [modifiers counter]}
   eid
   {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc eid :entity/temp-modifier]
     [:tx/mod-remove eid modifiers]]))

(defn info-text [[_ {:keys [counter]}] {:keys [ctx/elapsed-time]}]
  (str "Spiderweb - remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
