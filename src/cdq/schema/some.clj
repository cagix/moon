(ns cdq.schema.some
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/some [[_ & params] _schemas]
  :some)
