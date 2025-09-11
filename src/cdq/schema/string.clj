(ns cdq.schema.string
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/string [_ _schemas]
  :string)
