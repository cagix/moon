(ns cdq.entity.temp-modifier
  (:require [cdq.entity :as entity]))

(defn draw [_ entity _ctx]
  [[:draw/filled-circle (entity/position entity) 0.5 [0.5 0.5 0.5 0.4]]])
