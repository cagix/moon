(ns ^:no-doc anvil.entity.faction
  (:require [clojure.component :refer [defcomponent]]))

(defcomponent :entity/faction
  (component/info [faction _c]
    (str "Faction: " (name faction))))
