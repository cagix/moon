(ns cdq.entity
  (:require [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.op :as op]
            [cdq.malli :as m]
            [cdq.raycaster :as raycaster]
            [cdq.val-max :as val-max]
            [gdl.graphics.camera :as camera]))

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

(defmulti  render-below! (fn [[k] entity draw] k))
(defmethod render-below! :default [_ _entity draw])

(defmulti  render-default! (fn [[k] entity draw] k))
(defmethod render-default! :default [_ _entity draw])

(defmulti  render-above! (fn [[k] entity draw] k))
(defmethod render-above! :default [_ _entity draw])

(defmulti  render-info! (fn [[k] entity draw] k))
(defmethod render-info! :default [_ _entity draw])

(defn mod-value [base-value modifiers modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (op/apply (modifier-k modifiers)
            base-value))

(defn stat [entity k]
  (when-let [base-value (k entity)]
    (mod-value base-value
               (:entity/modifiers entity)
               (keyword "modifier" (name k)))))

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

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana-val entity))))

(defn skill-usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill} effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effect/some-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [viewport entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (camera/position (:camera viewport))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (:width viewport))  2)))
     (<= ydist (inc (/ (float (:height viewport)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? ctx/world-viewport target))
       (not (and los-checks?
                 (raycaster/blocked? ctx/raycaster
                                     (:position source)
                                     (:position target))))))
