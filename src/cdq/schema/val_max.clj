(ns cdq.schema.val-max
  (:require [cdq.val-max :as val-max]))

(defn malli-form [_ _schemas]
  val-max/schema)
