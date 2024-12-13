(ns anvil.entity.faction
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [gdl.utils :refer [defmethods defn-impl]]))

(defmethods :entity/faction
  (component/info [faction]
    (str "Faction: " (name faction))))

(defn-impl entity/enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))
