(ns cdq.entity.temp-modifier
  (:require [cdq.timer :as timer]
            [gdl.utils :as utils]))

(defn tick
  [{:keys [modifiers counter]}
   eid
   {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc     eid :entity/temp-modifier]
     [:tx/mod-remove eid modifiers]]))

(defn draw [_ entity _ctx]
  [[:draw/filled-circle (:body/position (:entity/body entity)) 0.5 [0.5 0.5 0.5 0.4]]])

(defn info-text [[_ {:keys [counter]}] {:keys [world/elapsed-time]}]
  (str "Spiderweb - remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
