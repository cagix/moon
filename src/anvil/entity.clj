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
