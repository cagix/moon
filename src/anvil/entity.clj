(ns anvil.entity
  (:require [data.grid2d :as g2d]
            [gdl.math.vector :as v]
            [gdl.math.shapes :as shape]))

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

(defn event
  ([c eid event])
  ([c eid event params]))

(def empty-inventory
  (->> #:inventory.slot{:bag      [6 4]
                        :weapon   [1 1]
                        :shield   [1 1]
                        :helm     [1 1]
                        :chest    [1 1]
                        :leg      [1 1]
                        :glove    [1 1]
                        :boot     [1 1]
                        :cloak    [1 1]
                        :necklace [1 1]
                        :rings    [2 1]}
       (map (fn [[slot [width height]]]
              [slot (g2d/create-grid width
                                     height
                                     (constantly nil))]))
       (into {})))

(defn valid-slot? [[slot _] item])

(defn set-item [eid cell item])

(defn remove-item [c eid cell])

(defn stackable? [item-a item-b])

(defn stack-item [c eid cell item])

(defn can-pickup-item? [{:keys [entity/inventory]} item])

(defn pickup-item [c eid item])

(defn stat [entity k])

(defn mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity])

(defn mana-val [entity])

(defn pay-mana-cost [entity cost])

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

(defn hitpoints
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [entity])

(defn mod-add    [entity mod])
(defn mod-remove [entity mod])
(defn mod-value  [base-value entity modifier-k])
(defn apply-max-modifier [val-max entity modifier-k])
(defn apply-min-modifier [val-max entity modifier-k])
