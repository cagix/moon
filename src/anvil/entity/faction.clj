(ns ^:no-doc anvil.entity.faction
  (:require [anvil.info :as info]
            [clojure.utils :refer [defmethods]]))

(defmethods :entity/faction
  (info/segment [faction _c]
    (str "Faction: " (name faction))))
