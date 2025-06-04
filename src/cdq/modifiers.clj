(ns cdq.modifiers
  (:refer-clojure :exclude [remove])
  (:require [cdq.op :as op]
            [cdq.val-max :as val-max]
            [cdq.malli :as m]))

(defn- get-value [base-value modifiers modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (op/apply (modifier-k modifiers)
            base-value))

(defn get-stat-value [stats stat-k]
  (when-let [base-value (stat-k stats)]
    (get-value base-value
               (:entity/modifiers stats)
               (keyword "modifier" (name stat-k)))))

(defn- add*    [mods other-mods] (merge-with op/add    mods other-mods))
(defn- remove* [mods other-mods] (merge-with op/remove mods other-mods))

(defn add    [stats mods] (update stats :entity/modifiers add*    mods))
(defn remove [stats mods] (update stats :entity/modifiers remove* mods))

(defn- ->pos-int [val-max]
  (mapv #(-> % int (max 0)) val-max))

; TODO can just pass ops instead of modifiers modifier-k
(defn- apply-max [val-max modifiers modifier-k]
  (assert (m/validate val-max/schema val-max) val-max)
  (let [val-max (update val-max 1 get-value modifiers modifier-k)
        [v mx] (->pos-int val-max)
        result [(min v mx) mx]]
  (assert (m/validate val-max/schema result) result)
  result))

; TODO can just pass ops instead of modifiers modifier-k
(defn- apply-min [val-max modifiers modifier-k]
  (assert (m/validate val-max/schema val-max) val-max)
  (let [val-max (update val-max 0 get-value modifiers modifier-k)
        [v mx] (->pos-int val-max)
        result [v (max v mx)]]
    (assert (m/validate val-max/schema result) result)
    result))

(defn get-mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [{:keys [entity/mana
           entity/modifiers]}]
  (apply-max mana modifiers :modifier/mana-max))

(defn mana-val [stats]
  (if (:entity/mana stats)
    ((get-mana stats) 0) ; TODO fucking optional shit
    0))

(defn not-enough-mana? [stats {:keys [skill/cost]}]
  (and cost (> cost (mana-val stats))))

(defn pay-mana-cost [stats cost]
  (let [mana-val (mana-val stats)]
    (assert (<= cost mana-val))
    (assoc-in stats [:entity/mana 0] (- mana-val cost))))

(defn get-hitpoints
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [{:keys [entity/hp
           entity/modifiers]}]
  (apply-max hp modifiers :modifier/hp-max))

(defn damage
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (apply-min (:entity/modifiers source) :modifier/damage-deal-min)
                (apply-max (:entity/modifiers source) :modifier/damage-deal-max))))

  ([source target damage]
   (update (damage source damage)
           :damage/min-max
           apply-max
           (:entity/modifiers target)
           :modifier/damage-receive-max)))
