(ns cdq.modifiers
  (:refer-clojure :exclude [remove])
  (:require [cdq.op :as op]
            [cdq.malli :as m]
            [cdq.val-max :as val-max]))

(defn get-value [base-value modifiers modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (op/apply (modifier-k modifiers)
            base-value))

(defn add    [mods other-mods] (merge-with op/add    mods other-mods))
(defn remove [mods other-mods] (merge-with op/remove mods other-mods))

(defn- ->pos-int [val-max]
  (mapv #(-> % int (max 0)) val-max))

(defn- apply-max [val-max modifiers modifier-k]
  {:pre  [(m/validate val-max/schema val-max)]
   :post [(m/validate val-max/schema val-max)]}
  (let [val-max (update val-max 1 get-value modifiers modifier-k)
        [v mx] (->pos-int val-max)]
    [(min v mx) mx]))

(defn- apply-min [val-max modifiers modifier-k]
  {:pre  [(m/validate val-max/schema val-max)]
   :post [(m/validate val-max/schema val-max)]}
  (let [val-max (update val-max 0 get-value modifiers modifier-k)
        [v mx] (->pos-int val-max)]
    [v (max v mx)]))

(defn get-mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [{:keys [entity/mana
           entity/modifiers]}]
  (apply-max mana modifiers :modifier/mana-max))

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
