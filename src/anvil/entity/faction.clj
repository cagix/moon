(ns anvil.entity.faction
  (:require [anvil.component :as component]
            [gdl.utils :refer [defmethods]]))

(defmethods :entity/faction
  (component/info [faction]
    (str "Faction: " (name faction))))

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))
