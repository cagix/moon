(ns moon.entity.species
  (:require [clojure.string :as str]))

(defn info [species]
  (str "[LIGHT_GRAY]Creature - " (str/capitalize (name species)) "[]"))
