(ns cdq.schema.val-max
  (:require [cdq.schema :as schema]
            [cdq.val-max :as val-max]))

(defmethod schema/malli-form :s/val-max [_ _schemas]
  val-max/schema)
