(ns cdq.schema)

(defn get-type [schema]
  (assert (vector? schema))
  (schema 0))

(defmulti create-value (fn [schema _v _db]
                         (get-type schema)))

(defmethod create-value :default [_schema v _db]
  v)

(defmulti malli-form (fn [schema _schemas]
                       (get-type schema)))

(defmethod malli-form :default [schema _schemas]
  schema)
