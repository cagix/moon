(ns anvil.entity
  (:require [anvil.action-bar :as action-bar]
            [anvil.effect :as effect]
            [anvil.inventory :as inventory :refer [valid-slot?]]
            [anvil.world :refer [timer reset-timer]]
            [anvil.modifiers :as mods]
            [clojure.utils :refer [find-first]]))

(defn damage-mods
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (mods/apply-min-modifier source :modifier/damage-deal-min)
                (mods/apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (damage-mods source damage)
           :damage/min-max
           mods/apply-max-modifier
           target
           :modifier/damage-receive-max)))

(defn hitpoints
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [entity]
  (-> entity
      :entity/hp
      (mods/apply-max-modifier entity :modifier/hp-max)))

(defn mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity]
  (-> entity
      :entity/mana
      (mods/apply-max-modifier entity :modifier/mana-max)))

(defn mana-value [entity]
  (if (:entity/mana entity)
    ((mana entity) 0)
    0))

(defn pay-mana-cost [entity cost]
  (let [mana-val ((mana entity) 0)]
    (assert (<= cost mana-val))
    (assoc-in entity [:entity/mana 0] (- mana-val cost))))

(defn stat-value [entity k]
  (when-let [base-value (k entity)]
    (mods/->value base-value
                  entity
                  (keyword "modifier" (name k)))))

(defn add-string-effect [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter reset-timer))
           {:text text
            :counter (timer 0.4)})))

(defn has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

(defn add-skill [entity {:keys [property/id] :as skill}]
  {:pre [(not (has-skill? entity skill))]}
  (when (:entity/player? entity)
    (action-bar/add-skill skill))
  (assoc-in entity [:entity/skills id] skill))

(defn remove-skill [entity {:keys [property/id] :as skill}]
  {:pre [(has-skill? entity skill)]}
  (when (:entity/player? entity)
    (action-bar/remove-skill skill))
  (update entity :entity/skills dissoc id))

(defn- applies-modifiers? [[slot _]]
  (not= :inventory.slot/bag slot))

(defn set-item [eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (valid-slot? cell item)))
    (when (:entity/player? entity)
      (inventory/set-item-image-in-widget cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (applies-modifiers? cell)
      (swap! eid mods/add (:entity/modifiers item)))))

(defn remove-item [eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      (inventory/remove-item-from-widget cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (applies-modifiers? cell)
      (swap! eid mods/remove (:entity/modifiers item)))))

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

(defn stackable? [item-a item-b]
  (and (:count item-a)
       (:count item-b) ; this is not required but can be asserted, all of one name should have count if others have count
       (= (:property/id item-a) (:property/id item-b))))

; TODO no items which stack are available
(defn stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item eid cell)
            (set-item eid cell (update cell-item :count + (:count item))))))

(defn- cells-and-items [inventory slot]
  (for [[position item] (slot inventory)]
    [[slot position] item]))

(defn- free-cell [inventory slot item]
  (find-first (fn [[_cell cell-item]]
                (or (stackable? item cell-item)
                    (nil? cell-item)))
              (cells-and-items inventory slot)))

(defn can-pickup-item? [{:keys [entity/inventory]} item]
  (or
   (free-cell inventory (:item/slot item)   item)
   (free-cell inventory :inventory.slot/bag item)))

(defn pickup-item [eid item]
  (let [[cell cell-item] (can-pickup-item? @eid item)]
    (assert cell)
    (assert (or (stackable? item cell-item)
                (nil? cell-item)))
    (if (stackable? item cell-item)
      (stack-item eid cell item)
      (set-item   eid cell item))))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana-value entity))))

(defn skill-usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill} effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effect/applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))
