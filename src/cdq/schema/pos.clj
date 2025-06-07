(ns cdq.schema.pos
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/pos [_ _schemas]
  pos?)
