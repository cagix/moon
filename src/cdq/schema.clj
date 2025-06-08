(ns cdq.schema
  (:refer-clojure :exclude [type]))

(defn type [schema]
  (cond
   (vector? schema) (schema 0)
   (keyword? schema) schema
   :else (throw (IllegalArgumentException. (str "Unkown schema type: " (class schema))))))

(defmulti malli-form (fn [schema _schemas] (type schema)))
(defmethod malli-form :default [schema _schemas] schema)
