(ns anvil.entity.hitpoints
  (:require [anvil.entity.modifiers :as mods]))

(defn ->value
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [entity]
  (-> entity
      :entity/hp
      (mods/apply-max-modifier entity :modifier/hp-max)))
