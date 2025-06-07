(ns cdq.schema.pos-int
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/pos-int [_ _schemas]
  pos-int?)
