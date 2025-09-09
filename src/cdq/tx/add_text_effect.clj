(ns cdq.tx.add-text-effect
  (:require [cdq.timer :as timer]))

(defn do! [[_ eid text duration] {:keys [ctx/elapsed-time]}]
  [[:tx/assoc
    eid
    :entity/string-effect
    (if-let [existing (:entity/string-effect @eid)]
      (-> existing
          (update :text str "\n" text)
          (update :counter timer/increment duration))
      {:text text
       :counter (timer/create elapsed-time duration)})]])
