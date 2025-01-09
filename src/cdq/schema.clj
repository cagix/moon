(ns cdq.schema
  (:refer-clojure :exclude [type])
  (:require [clojure.utils :refer [safe-get]]
            [cdq.property :as property]
            [cdq.malli :as m]))

(defn type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defmulti malli-form (fn [schema _schemas] (type schema)))
(defmethod malli-form :default [schema _schemas] schema)

; TODO here only namespace 'schema', not knowing about 'schemas' ??
; or namespace 'schemas' ?!
; abstraction over whole 'schemas' ?!
(defn property-types [schemas]
  (filter #(= "properties" (namespace %))
          (keys schemas)))

; TODO ... malli is internal to 'schema' ns ?!
; (m/generate (db/malli-form schema) {:size 3})
(defn schema-of [schemas k]
  (assert (contains? schemas k)
          (pr-str k))
  (safe-get schemas k))

(defn validate! [schemas property]
  (m/validate! (malli-form (schema-of schemas (property/type property))
                           schemas)
               property))
