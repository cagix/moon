(ns anvil.entity.string-effect
  (:require [anvil.world.time :refer [timer reset-timer]]))

(defn add [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter reset-timer))
           {:text text
            :counter (timer 0.4)})))
