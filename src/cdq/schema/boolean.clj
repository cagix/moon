(ns cdq.schema.boolean
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/boolean [[_ & params] _schemas]
  :boolean)
