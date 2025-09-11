(ns cdq.schema.vector
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/vector [[_ & params] _schemas]
  (apply vector :vector params))
