(ns cdq.schema.qualified-keyword
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/qualified-keyword [[_ & params] _schemas]
  (apply vector :qualified-keyword params))
