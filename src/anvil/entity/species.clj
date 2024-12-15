(ns ^:no-doc anvil.entity.species
  (:require [anvil.component :as component]
            [clojure.string :as str]
            [gdl.utils :refer [defmethods]]))

(defmethods :entity/species
  (component/info [[_ species]]
    (str "Creature - " (str/capitalize (name species)))))
