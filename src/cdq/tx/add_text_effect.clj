(ns cdq.tx.add-text-effect
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.timer :as timer]))

(defn- add-text-effect* [entity text {:keys [ctx/elapsed-time]}]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter #(timer/reset elapsed-time %)))
           {:text text
            :counter (timer/create elapsed-time 0.4)})))

(defmethod do! :tx/add-text-effect [[_ eid text] ctx]
  (swap! eid add-text-effect* text ctx)
  nil)
