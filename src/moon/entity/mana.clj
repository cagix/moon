(ns moon.entity.mana
  (:require [moon.val-max :as val-max]))

(defn value
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity]
  (-> entity
      :stats/mana
      (val-max/apply-max-modifier entity :modifier/mana-max)))
