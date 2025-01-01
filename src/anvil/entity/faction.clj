(ns ^:no-doc anvil.entity.faction
  (:require [gdl.info :as info]
            [clojure.component :refer [defcomponent]]))

(defcomponent :entity/faction
  (info/segment [faction _c]
    (str "Faction: " (name faction))))
