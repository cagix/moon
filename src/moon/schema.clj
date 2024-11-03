(ns moon.schema
  (:refer-clojure :exclude [type])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor]
            [gdl.utils :refer [safe-get truncate ->edn-str]]))

(def schemas (-> "schema.edn"
                 io/resource
                 slurp
                 edn/read-string))

(defn of [k]
  (safe-get schemas k))

(defn type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defmulti form type)
(defmethod form :default [schema] schema)

(def form-of (comp form of))

(defmulti edn->value (fn [schema v]
                       (when schema  ; undefined-data-ks
                         (type schema))))
(defmethod edn->value :default [_schema v] v)

(defn- widget-type [schema _]
  (let [stype (type schema)]
    (cond
     (#{:s/map-optional :s/components-ns} stype)
     :s/map

     (#{:s/number :s/nat-int :s/int :s/pos :s/pos-int :s/val-max} stype)
     :s/number

     :else stype)))

(defmulti widget        widget-type)
(defmulti widget-value  widget-type)

(defmethod widget :default [_ v]
  (ui/label (truncate (->edn-str v) 60)))

(defmethod widget-value :default [_ widget]
  ((actor/id widget) 1))
