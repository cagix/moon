(ns ^:no-doc anvil.entity.faction
  (:require [anvil.component :as component]
            [clojure.utils :refer [defmethods]]))

(defmethods :entity/faction
  (component/info [faction _c]
    (str "Faction: " (name faction))))
