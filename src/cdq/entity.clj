(ns cdq.entity
  (:require [clojure.gdx.math.vector2 :as v]
            [gdl.context :as c]
            [cdq.context.timer :as timer]
            [cdq.malli :as m]
            [cdq.math.shapes :as shape]
            [gdl.utils :refer [defsystem safe-merge]]
            [cdq.inventory :as inventory]
            [cdq.operation :as op]))

(defn mod-value [base-value {:keys [entity/modifiers]} modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (op/apply (modifier-k modifiers)
            base-value))

(defn stat [entity k]
  (when-let [base-value (k entity)]
    (mod-value base-value
               entity
               (keyword "modifier" (name k)))))

; temporary here, move to entity.render
; widgets in cdq.context and circular dependencies
(defsystem draw-gui-view)
(defmethod draw-gui-view :default [_ c])

(defsystem create)
(defmethod create :default [[_ v] _context]
  v)

(defmethod create :entity/delete-after-duration
  [[_ duration] c]
  (timer/create c duration))

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
  [[_ eid [skill effect-ctx]] c]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 (timer/create c))})

(defmethod create :npc-dead
  [[_ eid] c]
  {:eid eid})

(defmethod create :npc-idle
  [[_ eid] c]
  {:eid eid})

(defmethod create :npc-moving
  [[_ eid movement-vector] c]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer/create c (* (stat @eid :entity/reaction-time) 0.016))})

(defmethod create :npc-sleeping
  [[_ eid] c]
  {:eid eid})

(defmethod create :player-dead
  [[k] c]
  (c/build c :player-dead/component.enter))

(defmethod create :player-idle
  [[_ eid] c]
  (safe-merge (c/build c :player-idle/clicked-inventory-cell)
              {:eid eid}))

(defmethod create :player-item-on-cursor
  [[_ eid item] c]
  (safe-merge (c/build c :player-item-on-cursor/component)
              {:eid eid
               :item item}))

(defmethod create :player-moving
  [[_ eid movement-vector] c]
  {:eid eid
   :movement-vector movement-vector})

(defmethod create :stunned
  [[_ eid duration] c]
  {:eid eid
   :counter (timer/create c duration)})

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

(defn apply-max-modifier [val-max entity modifier-k]
  {:pre  [(m/validate m/val-max-schema val-max)]
   :post [(m/validate m/val-max-schema val-max)]}
  (let [val-max (update val-max 1 mod-value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [(min v mx) mx]))

(defn apply-min-modifier [val-max entity modifier-k]
  {:pre  [(m/validate m/val-max-schema val-max)]
   :post [(m/validate m/val-max-schema val-max)]}
  (let [val-max (update val-max 0 mod-value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [v (max v mx)]))

(defn can-pickup-item? [{:keys [entity/inventory]} item]
  (or
   (inventory/free-cell inventory (:item/slot item)   item)
   (inventory/free-cell inventory :inventory.slot/bag item)))

(defn mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity]
  (-> entity
      :entity/mana
      (apply-max-modifier entity :modifier/mana-max)))

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
  [entity]
  (-> entity
      :entity/hp
      (apply-max-modifier entity :modifier/hp-max)))

(defn damage
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (apply-min-modifier source :modifier/damage-deal-min)
                (apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (damage source damage)
           :damage/min-max
           apply-max-modifier
           target
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
