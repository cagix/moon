(ns cdq.schema
  (:require [clojure.gdx.scenes.scene2d.actor :as actor]))

(defn get-type [schema]
  (assert (vector? schema))
  (schema 0))

(defmulti create-value (fn [schema _v _db]
                         (get-type schema)))

(defmethod create-value :default [_schema v _db]
  v)

(defmulti malli-form (fn [schema _schemas]
                       (get-type schema)))

(declare k->methods)

(defn create [schema attribute v ctx]
  (if-let [f (:create (k->methods (get-type schema)))]
    (f schema attribute v ctx)
    ((:create (k->methods :default)) schema attribute v ctx)))

(defn value [schema attribute widget schemas]
  (if-let [f (:value (k->methods (get-type schema)))]
    (f schema attribute widget schemas)
    ((:value (k->methods :default)) schema attribute widget schemas)))

(defn build [ctx schema k v]
  (let [widget (actor/build? (create schema k v ctx))]
    (actor/set-user-object! widget [k v])
    widget))
