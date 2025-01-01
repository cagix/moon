(ns ^:no-doc anvil.entity.species
  (:require [clojure.component :refer [defcomponent]]
            [clojure.string :as str]))

(defcomponent :entity/species
  (component/segment [[_ species] _c]
    (str "Creature - " (str/capitalize (name species)))))
