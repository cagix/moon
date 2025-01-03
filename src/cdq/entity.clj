(ns cdq.entity
  (:require [cdq.inventory :as inventory]
            [clojure.component :as component :refer [defcomponent]]
            [gdl.context :refer [set-cursor]]
            [gdl.operation :as op]
            [gdl.malli :as m]
            [gdl.math.vector :as v]
            [gdl.math.shapes :as shape]
            [reduce-fsm :as fsm]))

(defn direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

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

(defn- send-event! [c eid event params]
  (when-let [fsm (:entity/fsm @eid)]
    (let [old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (state-obj @eid)
              new-state-obj [new-state-k (component/create (if params
                                                             [new-state-k eid params]
                                                             [new-state-k eid])
                                                        c)]]
          (when (:entity/player? @eid)
            (when-let [cursor (component/cursor new-state-obj)]
              (set-cursor c cursor)))
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (component/exit  old-state-obj c)
          (component/enter new-state-obj c))))))

(defn event
  ([c eid event]
   (send-event! c eid event nil))
  ([c eid event params]
   (send-event! c eid event params)))

(defn- mods-add    [mods other-mods] (merge-with op/add    mods other-mods))
(defn- mods-remove [mods other-mods] (merge-with op/remove mods other-mods))

(defn mod-add    [entity mods] (update entity :entity/modifiers mods-add    mods))
(defn mod-remove [entity mods] (update entity :entity/modifiers mods-remove mods))

(defn mod-value [base-value {:keys [entity/modifiers]} modifier-k]
  {:pre [(= "modifier" (namespace modifier-k))]}
  (op/apply (modifier-k modifiers)
            base-value))

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

(defn notify-controller-item-set [context entity cell item])

(defn notify-controller-item-removed [context entity cell])

(defn set-item [c eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (notify-controller-item-set c entity cell item)
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid mod-add (:entity/modifiers item)))))

(defn remove-item [c eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (notify-controller-item-removed c entity cell)
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid mod-remove (:entity/modifiers item)))))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [eid cell]
  (let [item (get-in (:entity/inventory @eid) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (remove-item! eid cell)
       (set-item! eid cell (update item :count dec)))
      (remove-item! eid cell))))

; TODO no items which stack are available
(defn stack-item [c eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item c eid cell)
            (set-item c eid cell (update cell-item :count + (:count item))))))

(defn can-pickup-item? [{:keys [entity/inventory]} item]
  (or
   (inventory/free-cell inventory (:item/slot item)   item)
   (inventory/free-cell inventory :inventory.slot/bag item)))

(defn pickup-item [c eid item]
  (let [[cell cell-item] (can-pickup-item? @eid item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (stack-item c eid cell item)
      (set-item c eid cell item))))

(defn stat [entity k]
  (when-let [base-value (k entity)]
    (mod-value base-value
               entity
               (keyword "modifier" (name k)))))

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
