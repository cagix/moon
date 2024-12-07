(ns forge.entity.string-effect
  (:require [forge.world.time :refer [timer reset-timer]]))

(defn add [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter reset-timer))
           {:text text
            :counter (timer 0.4)})))
