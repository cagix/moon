(ns clojure.schema
  (:refer-clojure :exclude [type])
  (:require [clojure.malli :as m]))

(defn type [schema]
  (cond
   (vector? schema) (schema 0)
   (keyword? schema) schema
   :else (throw (IllegalArgumentException. (str "Unkown schema type: " (class schema))))))

(defmulti malli-form (fn [schema _schemas] (type schema)))
(defmethod malli-form :default [schema _schemas] schema)

(defn schema-of [schemas k]
  (assert (contains? schemas k) (pr-str k))
  (get schemas k))

(defn validate! [schemas schema-k value]
  (m/validate! (malli-form (schema-of schemas schema-k)
                           schemas)
               value))

(defn optional-k? [k schema schemas]
  (m/optional? k (malli-form schema schemas)))

(defn generate [schema size schemas]
  (m/generate (malli-form schema schemas) {:size 3}))

(defn map-keys [schema schemas]
  (m/map-keys (malli-form schema schemas)))

(defn optional-keys-left [schema m schemas]
  (m/optional-keys-left (malli-form schema schemas) m))

(comment
 (require '[clojure.spec.alpha :as s])

 (s/def ::val-max
   (s/and
    (s/coll-of (s/int-in 0 Integer/MAX_VALUE) :kind vector? :count 2)
    (fn [[v mx]] (<= v mx))))

 (defn explain-str [spec value]
   (with-out-str (s/explain spec value)))

 (defn validate! [spec data]
   (when-not (s/valid? spec data)
     (throw (ex-info (str "Validation failed: " (explain-str spec data))
                     {:value data
                      :spec spec}))))

 (validate! ::val-max [0.5 1])

 )
