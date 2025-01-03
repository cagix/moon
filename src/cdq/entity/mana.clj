(ns cdq.entity.mana
  (:require [cdq.entity :as entity]))

(defn create [[_ v] _c]
  [v v])

(defn info [_ entity _c]
  (str "Mana: " (entity/mana entity)))
