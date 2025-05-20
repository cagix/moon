(ns cdq.tx.add-text-effect
  (:require [cdq.timer :as timer]))

(defn- add-text-effect [entity text elapsed-time]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter #(timer/reset elapsed-time %)))
           {:text text
            :counter (timer/create elapsed-time 0.4)})))

(defn do! [{:keys [ctx/elapsed-time]} eid text]
  (swap! eid add-text-effect text elapsed-time))
