(ns cdq.entity.species
  (:require [clojure.string :as str]))

(defn info-text [[_ species] _ctx]
  (str "Creature - " (str/capitalize (name species))))
