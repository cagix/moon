(ns cdq.db.schema
  (:require [cdq.db :as db]
            [cdq.db.schemas :as schemas]))

(defmulti create-value (fn [[k] _v _db]
                         k))

(defmethod create-value :default [_ v _db]
  v)

(defmethod create-value :s/one-to-many [_ property-ids db]
  (set (map (partial db/build db) property-ids)))

(defmethod create-value :s/one-to-one [_ property-id db]
  (db/build db property-id))

(defmethod create-value :s/map [_ v db]
  (schemas/build-values (:db/schemas db) v db))
