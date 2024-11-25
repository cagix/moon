(ns forge.schema
  (:refer-clojure :exclude [type])
  (:require [gdl.utils :refer [safe-get]]))

(declare schemas)

(defn of [k]
  (safe-get schemas k))

(defn type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defmulti form type)
(defmethod form :default [schema] schema)

(def form-of (comp form of))
