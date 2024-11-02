(ns moon.entity.faction
  (:require [moon.component :as component]))

(defmethods :entity/faction
  {:let faction}
  (component/info [_]
    (str "[SLATE]Faction: " (name faction) "[]")))

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))
