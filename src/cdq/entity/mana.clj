(ns cdq.entity.mana
  (:require [cdq.entity :as entity]
            [gdl.info :as info]))

(defn create [[_ v] _c]
  [v v])

(defn info [_ _c]
  (str "Mana: " (entity/mana info/*info-text-entity*)))
