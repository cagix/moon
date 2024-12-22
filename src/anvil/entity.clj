(ns anvil.entity)

(defn direction [entity other-entity])

(defn collides? [entity other-entity])

(defn tile [entity])

(defn enemy [entity])

(defn state-k [entity])

(defn state-obj [entity])

(defn event
  ([eid event])
  ([eid event params]))

(defn valid-slot? [[slot _] item])

(defn set-item [eid cell item])

(defn remove-item [eid cell])

(defn stackable? [item-a item-b])

(defn stack-item [eid cell item])

(defn can-pickup-item? [{:keys [entity/inventory]} item])

(defn pickup-item [eid item])
