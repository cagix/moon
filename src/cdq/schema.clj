(ns cdq.schema
  (:refer-clojure :exclude [type]))

(defn type [schema]
  (cond
   (vector? schema) (schema 0)
   (keyword? schema) schema
   :else (throw (IllegalArgumentException. (str "Unkown schema type: " (class schema))))))

(defmulti edn->value (fn [schema v _ctx]
                       (when schema  ; undefined-data-ks
                         (type schema))))

(defmethod edn->value :default [_schema v _ctx]
  v)
