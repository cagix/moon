(ns clojure.schema
  (:refer-clojure :exclude [type])
  (:require [clojure.utils :refer [safe-get]]
            [clojure.malli :as m]))

(defn type [schema]
  (cond
   (vector? schema) (schema 0)
   (keyword? schema) schema
   :else (throw (IllegalArgumentException. (str "Unkown schema type: " (class schema))))))

(defmulti malli-form (fn [schema _schemas] (type schema)))
(defmethod malli-form :default [schema _schemas] schema)

(defn property-types [schemas]
  (filter #(= "properties" (namespace %))
          (keys schemas)))

(defn schema-of [schemas k]
  (assert (contains? schemas k)
          (pr-str k))
  (safe-get schemas k))

(defn validate! [schemas schema-k value]
  (m/validate! (malli-form (schema-of schemas schema-k)
                           schemas)
               value))
