(ns cdq.entity
  (:require [cdq.entity.stats.op :as op]
            [cdq.math :as math]
            [cdq.math.vector2 :as v]
            [cdq.val-max :as val-max]
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

(defmulti  render-below! (fn [[k] entity g] k))
(defmethod render-below! :default [_ _entity g])

(defmulti  render-default! (fn [[k] entity g] k))
(defmethod render-default! :default [_ _entity g])

(defmulti  render-above! (fn [[k] entity g] k))
(defmethod render-above! :default [_ _entity g])

(defmulti  render-info! (fn [[k] entity g] k))
(defmethod render-info! :default [_ _entity g])

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

(defn pay-mana-cost [entity cost]
  (let [mana-val ((mana entity) 0)]
    (assert (<= cost mana-val))
    (assoc-in entity [:entity/mana 0] (- mana-val cost))))

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

; TODO use at projectile & also adjust rotation
(defn start-point [entity target*]
  (v/add (:position entity)
         (v/scale (direction entity target*)
                  (:radius entity))))

(defn end-point [entity target* maxrange]
  (v/add (start-point entity target*)
         (v/scale (direction entity target*)
                  maxrange)))

(defn in-range? [entity target* maxrange] ; == circle-collides?
  (< (- (float (v/distance (:position entity)
                           (:position target*)))
        (float (:radius entity))
        (float (:radius target*)))
     (float maxrange)))

(defn has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

; we cannot just set/unset movement direction
; because it is handled by the state enter/exit for npc/player movement state ...
; so we cannot expose it as a 'transaction'
; so the movement should be updated in the respective npc/player movement 'state' and no movement 'component' necessary !
; for projectiles inside projectile update !?
(defn set-movement [eid movement-vector]
  (swap! eid assoc :entity/movement {:direction movement-vector
                                     :speed (or (stat @eid :entity/movement-speed) 0)}))

(defn mark-destroyed [eid]
  (swap! eid assoc :entity/destroyed? true))

(defprotocol Entity
  (add-text-effect [_ text]))
