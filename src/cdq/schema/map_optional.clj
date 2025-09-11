(ns cdq.schema.map-optional
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/map-optional [[_ ks] schemas]
  (schema/malli-form [:s/map (map (fn [k] [k {:optional true}]) ks)]
                     schemas))
