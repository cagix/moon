(ns cdq.schema)

(defn get-type [schema]
  (assert (vector? schema))
  (schema 0))

(defmulti create-value (fn [schema _v _db]
                         (get-type schema)))

(defmethod create-value :default [_schema v _db]
  v)

(defmulti malli-form (fn [schema _schemas]
                       (get-type schema)))

(defmethod malli-form :default [schema _schemas]
  schema)

(defn widget-type
  [schema attribute]
  (let [stype (get-type schema)]
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
