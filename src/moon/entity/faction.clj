(ns moon.entity.faction
  (:require [moon.component :as component]))

(defc :entity/faction
  {:schema [:enum :good :evil]
   :let faction}
  (component/info [_]
    (str "[SLATE]Faction: " (name faction) "[]")))

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))
