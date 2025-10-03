(ns cdq.db.schema.val-max
  (:require [gdl.val-max :as val-max]))

(defn malli-form [_ _schemas]
  val-max/schema)

(defn create-value [_ v _db]
  v)
