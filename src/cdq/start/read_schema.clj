(ns cdq.start.read-schema
  (:require [cdq.malli :as m]))

(defn do!
  [ctx]
  (update ctx :ctx/schema m/schema))
