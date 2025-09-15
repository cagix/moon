(ns cdq.start.read-schema
  (:require [cdq.malli :as m]))

(defn do!
  [ctx schema-form]
  (assoc ctx :ctx/schema (m/schema schema-form)))
