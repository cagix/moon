(ns ^:no-doc anvil.entity.faction
  (:require [anvil.component :as component]))

(defmethods :entity/faction
  (component/info [faction _c]
    (str "Faction: " (name faction))))
