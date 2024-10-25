(ns moon.entity.faction
  (:require [moon.component :refer [defc] :as component]))

(defc :entity/faction
  {:schema [:enum :good :evil]
   :let faction}
  (component/info [_]
    (str "[SLATE]Faction: " (name faction) "[]")))
