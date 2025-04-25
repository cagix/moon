(ns cdq.entity
  (:require [cdq.math.vector2 :as v]
            [cdq.utils :refer [safe-merge]]
            [cdq.db :as db]
            [cdq.timer :as timer]
            [cdq.schema :as s]
            [cdq.math.shapes :as shape]
            [cdq.inventory :as inventory]
            [cdq.operation :as op]))

(defn mod-value [base-value modifiers modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (op/apply (modifier-k modifiers)
            base-value))

(defn stat [entity k]
  (when-let [base-value (k entity)]
    (mod-value base-value
               (:entity/modifiers entity)
               (keyword "modifier" (name k)))))

; temporary here, move to entity.render
; widgets in cdq.world and circular dependencies
(defmulti draw-gui-view (fn [[k] context]
                          k))
(defmethod draw-gui-view :default [_ c])

(defmulti create (fn [[k] context]
                   k))
(defmethod create :default [[_ v] _context]
  v)

(defmethod create :entity/delete-after-duration
  [[_ duration]
   {:keys [cdq.context/elapsed-time] :as c}]
  (timer/create elapsed-time duration))

(defmethod create :entity/hp
  [[_ v] _c]
  [v v])

(defmethod create :entity/mana
  [[_ v] _c]
  [v v])

(defmethod create :entity/projectile-collision
  [[_ v] c]
  (assoc v :already-hit-bodies #{}))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (stat entity (:skill/action-time-modifier-key skill))
         1)))

(defmethod create :active-skill
  [[_ eid [skill effect-ctx]]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 (timer/create elapsed-time))})

(defmethod create :npc-dead
  [[_ eid] c]
  {:eid eid})

(defmethod create :npc-idle
  [[_ eid] c]
  {:eid eid})

(defmethod create :npc-moving
  [[_ eid movement-vector]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer/create elapsed-time (* (stat @eid :entity/reaction-time) 0.016))})

(defmethod create :npc-sleeping
  [[_ eid] c]
  {:eid eid})

(defmethod create :player-dead
  [[k] {:keys [cdq/db] :as c}]
  (db/build db :player-dead/component.enter c))

(defmethod create :player-idle
  [[_ eid] {:keys [cdq/db] :as c}]
  (safe-merge (db/build db :player-idle/clicked-inventory-cell c)
              {:eid eid}))

(defmethod create :player-item-on-cursor
  [[_ eid item] {:keys [cdq/db] :as c}]
  (safe-merge (db/build db :player-item-on-cursor/component c)
              {:eid eid
               :item item}))

(defmethod create :player-moving
  [[_ eid movement-vector] c]
  {:eid eid
   :movement-vector movement-vector})

(defmethod create :stunned
  [[_ eid duration]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :counter (timer/create elapsed-time duration)})

(defn direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn collides? [entity other-entity]
  (shape/overlaps? entity other-entity))

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

(defn apply-max-modifier [val-max modifiers modifier-k]
  {:pre  [(s/validate s/val-max-schema val-max)]
   :post [(s/validate s/val-max-schema val-max)]}
  (let [val-max (update val-max 1 mod-value modifiers modifier-k)
        [v mx] (->pos-int val-max)]
    [(min v mx) mx]))

(defn apply-min-modifier [val-max modifiers modifier-k]
  {:pre  [(s/validate s/val-max-schema val-max)]
   :post [(s/validate s/val-max-schema val-max)]}
  (let [val-max (update val-max 0 mod-value modifiers modifier-k)
        [v mx] (->pos-int val-max)]
    [v (max v mx)]))

(defn can-pickup-item? [{:keys [entity/inventory]} item]
  (or
   (inventory/free-cell inventory (:item/slot item)   item)
   (inventory/free-cell inventory :inventory.slot/bag item)))

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

(defn add-text-effect [entity {:keys [cdq.context/elapsed-time]} text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter #(timer/reset % elapsed-time)))
           {:text text
            :counter (timer/create elapsed-time 0.4)})))
