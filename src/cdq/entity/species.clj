(ns cdq.entity.species
  (:require [clojure.string :as str]))

(defn text [[_ species] _entity _c]
  (str "Creature - " (str/capitalize (name species))))
