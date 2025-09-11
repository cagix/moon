(ns cdq.schema.nat-int
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/nat-int [_ _schemas] nat-int?)
