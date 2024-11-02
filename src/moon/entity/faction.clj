(ns moon.entity.faction
  (:require [moon.component :as component]))

(defmethods :entity/faction
  {:let faction}
  (component/info [_]
    (str "[SLATE]Faction: " (name faction) "[]")))
