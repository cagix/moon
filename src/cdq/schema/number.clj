(ns cdq.schema.number
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/number  [_ _schemas] number?)
