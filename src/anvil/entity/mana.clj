(ns anvil.entity.mana
  (:refer-clojure :exclude [val])
  (:require [anvil.entity.modifiers :as mods]))

(defn ->value
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity]
  (-> entity
      :entity/mana
      (mods/apply-max-modifier entity :modifier/mana-max)))

(defn val [entity]
  (if (:entity/mana entity)
    ((->value entity) 0)
    0))

(defn pay-cost [entity cost]
  (let [mana-val ((->value entity) 0)]
    (assert (<= cost mana-val))
    (assoc-in entity [:entity/mana 0] (- mana-val cost))))
