(ns cdq.schema.coll
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/coll [[_ k] schemas]
  [:sequential (schema/malli-form (get schemas k) schemas)])
