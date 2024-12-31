(ns ^:no-doc anvil.entity.species
  (:require [anvil.component :as component]
            [clojure.string :as str]
            [clojure.utils :refer [defmethods]]))

(defmethods :entity/species
  (component/info [[_ species] _c]
    (str "Creature - " (str/capitalize (name species)))))
