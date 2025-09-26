(ns cdq.entity.species
  (:require [clojure.string :as str]))

(defn info-text [[_ species] _world]
  (str "Creature - " (str/capitalize (name species))))
