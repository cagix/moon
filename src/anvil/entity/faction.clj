(ns ^:no-doc anvil.entity.faction
  (:require [anvil.component :as component]
            [anvil.entity :as entity]))

(defmethods :entity/faction
  (component/info [faction _c]
    (str "Faction: " (name faction))))

(defn-impl entity/enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))
