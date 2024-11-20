(ns moon.entity.mana
  (:require [moon.info :as info]
            [moon.entity :as entity]))

(defn info [_]
  (str "Mana: " (entity/mana info/*entity*)))

(defn ->v [v]
  [v v])
