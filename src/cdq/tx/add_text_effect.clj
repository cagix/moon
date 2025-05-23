(ns cdq.tx.add-text-effect
  (:require [cdq.g :as g]))

(defn- add-text-effect [entity text ctx]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter #(g/reset-timer ctx %)))
           {:text text
            :counter (g/create-timer ctx 0.4)})))

(defn do! [ctx eid text]
  (swap! eid add-text-effect text ctx))
