(ns ^:no-doc anvil.entity.species
  (:require [gdl.info :as info]
            [clojure.string :as str]
            [clojure.component :refer [defcomponent]]))

(defcomponent :entity/species
  (info/segment [[_ species] _c]
    (str "Creature - " (str/capitalize (name species)))))
