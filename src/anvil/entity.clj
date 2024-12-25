(ns anvil.entity)

(defn direction [entity other-entity])

(defn collides? [entity other-entity])

(defn tile [entity])

(defn enemy [entity])

(defn state-k [entity])

(defn state-obj [entity])

(defn event
  ([c eid event])
  ([c eid event params]))

(def empty-inventory)

(defn valid-slot? [[slot _] item])

(defn set-item [eid cell item])

(defn remove-item [eid cell])

(defn stackable? [item-a item-b])

(defn stack-item [eid cell item])

(defn can-pickup-item? [{:keys [entity/inventory]} item])

(defn pickup-item [eid item])

(defn stat [entity k])

(defn mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity])

(defn mana-val [entity])

(defn pay-mana-cost [entity cost])

(defn damage
  ([source damage])
  ([source target damage]))

(defn hitpoints
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [entity])

(defn mod-add    [entity mod])
(defn mod-remove [entity mod])
(defn mod-value  [base-value entity modifier-k])
(defn apply-max-modifier [val-max entity modifier-k])
(defn apply-min-modifier [val-max entity modifier-k])
