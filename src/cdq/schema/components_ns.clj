(ns cdq.schema.components-ns
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/components-ns [[_ ns-name-k] schemas]
  (schema/malli-form [:s/map-optional (filter #(= (name ns-name-k) (namespace %))
                                              (keys schemas))]
                     schemas))
