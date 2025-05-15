(ns cdq.tx.add-text-effect
  (:require [cdq.ctx :as ctx]
            [cdq.timer :as timer]))

(defn- add-text-effect [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter #(timer/reset ctx/elapsed-time %)))
           {:text text
            :counter (timer/create ctx/elapsed-time 0.4)})))

(defn do! [eid text]
  (swap! eid add-text-effect text))
