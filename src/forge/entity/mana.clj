(ns forge.entity.mana
  (:require [forge.modifiers :refer [apply-max-modifier]]
            [forge.world :refer [->v]]))

(defmethod ->v :entity/mana [[_ v]]
  [v v])

(defn e-mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity]
  (-> entity
      :entity/mana
      (apply-max-modifier entity :modifier/mana-max)))

(defn mana-value [entity]
  (if (:entity/mana entity)
    ((e-mana entity) 0)
    0))

(defn pay-mana-cost [entity cost]
  (let [mana-val ((e-mana entity) 0)]
    (assert (<= cost mana-val))
    (assoc-in entity [:entity/mana 0] (- mana-val cost))))
