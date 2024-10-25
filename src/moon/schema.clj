(ns moon.schema
  (:refer-clojure :exclude [type])
  (:require [moon.component :as component]
            [gdl.utils :refer [safe-get]]))

(defn of [k]
  (:schema (safe-get component/meta k)))

(defn type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defmulti form type)
(defmethod form :default [schema] schema)

(def form-of (comp form of))

(defn- widget-type [schema _]
  (let [stype (type schema)]
    (cond
     (#{:s/map-optional :s/components-ns} stype)
     :s/map

     (#{number? nat-int? int? pos? pos-int? :s/val-max} stype)
     number?

     :else stype)))

(defmulti widget        widget-type)
(defmulti widget-value  widget-type)

(defmulti edn->value (fn [schema v]
                       (when schema  ; undefined-data-ks
                         (type schema))))
(defmethod edn->value :default [_schema v] v)
