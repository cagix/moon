(ns cdq.schema
  (:refer-clojure :exclude [type])
  (:require [cdq.malli :as m]
            [cdq.utils :as utils]))

(defn type [schema]
  (cond
   (vector? schema) (schema 0)
   (keyword? schema) schema
   :else (throw (IllegalArgumentException. (str "Unkown schema type: " (class schema))))))

(defmulti create-value (fn [schema _v _db]
                         (type schema)))

(defmethod create-value :default [_schema v _db]
  v)

(defn widget-type
  [schema attribute]
  (let [stype (type schema)]
    (cond
     (= attribute :entity/animation)
     :widget/animation

     (= attribute :entity/image)
     :widget/image

     (#{:s/map-optional :s/components-ns} stype)
     :s/map

     (#{:s/number :s/nat-int :s/int :s/pos :s/pos-int :s/val-max} stype)
     :widget/edn

     :else stype)))

(defmulti malli-form (fn [schema _schemas] (type schema)))
(defmethod malli-form :default [schema _schemas] schema)

(defn build-values [schemas property db]
  (utils/apply-kvs property
                   (fn [k v]
                     (let [schema (get schemas k)
                           v (if (map? v)
                               (build-values schemas v db)
                               v)]
                       (try (create-value schema v db)
                            (catch Throwable t
                              (throw (ex-info " " {:k k :v v} t))))))))

(defn validate [schemas k value]
  (m/form->validate (malli-form (get schemas k) schemas)
                    value))

(defn map-keys [schemas map-schema]
  (m/map-keys (malli-form map-schema schemas)))

(defn optional-keyset [schemas map-schema]
  (m/optional-keyset (malli-form map-schema schemas)))

(defn optional-k? [schemas map-schema k]
  (m/optional? k (malli-form map-schema schemas)))

(defn k->default-value [schemas k]
  (let [schema (utils/safe-get schemas k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (m/generate (malli-form schema schemas)
                       {:size 3}))))
