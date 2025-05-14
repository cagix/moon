(ns cdq.schema
  (:refer-clojure :exclude [type])
  (:require [cdq.malli :as malli]
            [clojure.set :as set]))

(defn type [schema]
  (cond
   (vector? schema) (schema 0)
   (keyword? schema) schema
   :else (throw (IllegalArgumentException. (str "Unkown schema type: " (class schema))))))

(defmulti malli-form (fn [schema _schemas] (type schema)))
(defmethod malli-form :default [schema _schemas] schema)

(defmulti edn->value (fn [schema v]
                       (when schema  ; undefined-data-ks
                         (type schema))))

(defmethod edn->value :default [_schema v]
  v)

(defn map-keys [schema schemas]
  (malli/map-keys (malli-form schema schemas)))

(defn- map-form-k->properties
  "Given a map schema gives a map of key to key properties (like :optional)."
  [m-schema]
  (let [[_m _p & ks] m-schema]
    (into {} (for [[k m? _schema] ks]
               [k (if (map? m?) m?)]))))

(defn- optional? [k map-schema]
  (:optional (k (map-form-k->properties map-schema))))

(defn optional-k? [k schema schemas]
  (optional? k (malli-form schema schemas)))

(defn- optional-keyset [m-schema schemas]
  (set (filter #(optional? % m-schema) (map-keys m-schema schemas))))

(comment
 (= (optional-keyset
     [:map {:closed true}
      [:foo]
      [:bar]
      [:baz {:optional true}]
      [:boz {:optional false}]
      [:asdf {:optional true}]])
    [:baz :asdf])
 )

(defn optional-keys-left [schema m schemas]
  (seq (set/difference (optional-keyset (malli-form schema schemas)
                                        schemas)
                       (set (keys m)))))
