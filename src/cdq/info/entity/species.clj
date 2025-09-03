(ns cdq.info.entity.species
  (:require [clojure.string :as str]))

(defn info-segment [[_ species] _ctx]
  (str "Creature - " (str/capitalize (name species))))
