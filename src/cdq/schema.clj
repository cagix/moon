(ns cdq.schema
  (:refer-clojure :exclude [type]))

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
