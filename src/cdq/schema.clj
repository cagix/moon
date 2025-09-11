(ns cdq.schema
  (:require [cdq.utils :as utils]
            [clojure.gdx.scenes.scene2d.actor :as actor]))

(defn get-type [schema]
  (assert (vector? schema))
  (schema 0))

(defmulti create-value (fn [schema _v _db]
                         (get-type schema)))

(defmethod create-value :default [_schema v _db]
  v)

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

(defmulti malli-form (fn [schema _schemas]
                       (get-type schema)))

(declare k->methods)

(defn create [schema attribute v ctx]
  (let [f (:create (k->methods (get-type schema)))]
    (f schema attribute v ctx)))

(defn value [schema attribute widget schemas]
  (let [f (:value (k->methods (get-type schema)))]
    (f schema attribute widget schemas)))

(defn build [ctx schema k v]
  (let [widget (actor/build? (create schema k v ctx))]
    (actor/set-user-object! widget [k v])
    widget))
