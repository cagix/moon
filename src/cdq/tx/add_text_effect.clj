(ns cdq.tx.add-text-effect
  (:require [cdq.timer :as timer]))

(defn- add-text-effect [entity text duration elapsed-time]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter timer/increment duration))
           {:text text
            :counter (timer/create elapsed-time duration)})))

(defn do! [[_ eid text duration] {:keys [ctx/elapsed-time]}]
  (swap! eid add-text-effect text duration elapsed-time)
  nil)
