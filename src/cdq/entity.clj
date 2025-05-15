(ns cdq.entity
  (:require [cdq.op :as op]
            [cdq.math :as math]
            [cdq.val-max :as val-max]
            [cdq.vector2 :as v]
            [malli.core :as m]))

(defmulti create (fn [[k]]
                   k))
(defmethod create :default [[_ v]]
  v)

(defmulti create! (fn [[k] eid]
                    k))
(defmethod create! :default [_ eid])

(defmulti destroy! (fn [[k] eid]
                    k))
(defmethod destroy! :default [_ eid])

(defmulti tick! (fn [[k] eid]
                  k))
(defmethod tick! :default [_ eid])

(defmulti  render-below! (fn [[k] entity] k))
(defmethod render-below! :default [_ _entity])

(defmulti  render-default! (fn [[k] entity] k))
(defmethod render-default! :default [_ _entity])

(defmulti  render-above! (fn [[k] entity] k))
(defmethod render-above! :default [_ _entity])

(defmulti  render-info! (fn [[k] entity] k))
(defmethod render-info! :default [_ _entity])

(defn mod-value [base-value modifiers modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (op/apply (modifier-k modifiers)
            base-value))

(defn stat [entity k]
  (when-let [base-value (k entity)]
    (mod-value base-value
               (:entity/modifiers entity)
               (keyword "modifier" (name k)))))

(defn direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn collides? [entity other-entity]
  (math/overlaps? entity other-entity))

(defn tile [entity]
  (mapv int (:position entity)))

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))

(defn state-k [entity]
  (-> entity :entity/fsm :state))

(defn state-obj [entity]
  (let [k (state-k entity)]
    [k (k entity)]))

(defn- mods-add    [mods other-mods] (merge-with op/add    mods other-mods))
(defn- mods-remove [mods other-mods] (merge-with op/remove mods other-mods))

(defn mod-add    [entity mods] (update entity :entity/modifiers mods-add    mods))
(defn mod-remove [entity mods] (update entity :entity/modifiers mods-remove mods))

(defn- ->pos-int [val-max]
  (mapv #(-> % int (max 0)) val-max))

(defn- apply-max-modifier [val-max modifiers modifier-k]
  {:pre  [(m/validate val-max/schema val-max)]
   :post [(m/validate val-max/schema val-max)]}
  (let [val-max (update val-max 1 mod-value modifiers modifier-k)
        [v mx] (->pos-int val-max)]
    [(min v mx) mx]))

(defn- apply-min-modifier [val-max modifiers modifier-k]
  {:pre  [(m/validate val-max/schema val-max)]
   :post [(m/validate val-max/schema val-max)]}
  (let [val-max (update val-max 0 mod-value modifiers modifier-k)
        [v mx] (->pos-int val-max)]
    [v (max v mx)]))

(defn mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [{:keys [entity/mana
           entity/modifiers]}]
  (apply-max-modifier mana modifiers :modifier/mana-max))

(defn mana-val [entity]
  (if (:entity/mana entity)
    ((mana entity) 0)
    0))

(defn hitpoints
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [{:keys [entity/hp
           entity/modifiers]}]
  (apply-max-modifier hp modifiers :modifier/hp-max))

(defn damage
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (apply-min-modifier (:entity/modifiers source) :modifier/damage-deal-min)
                (apply-max-modifier (:entity/modifiers source) :modifier/damage-deal-max))))

  ([source target damage]
   (update (damage source damage)
           :damage/min-max
           apply-max-modifier
           (:entity/modifiers target)
           :modifier/damage-receive-max)))

(defprotocol Entity
  (in-range? [_ target maxrange]))
