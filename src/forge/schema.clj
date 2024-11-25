(ns forge.schema
  (:refer-clojure :exclude [type]))

; used @ default-value
; widget-type
; edn->value
; & form
(defn type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defmulti form type)
(defmethod form :default [schema] schema)
