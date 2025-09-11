(ns cdq.schema.int
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/int [_ _schemas]
  int?)
