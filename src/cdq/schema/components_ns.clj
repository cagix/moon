(ns cdq.schema.components-ns
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/components-ns [[_ ns-name-k] schemas]
  (schema/malli-form [:s/map (map (fn [k] [k {:optional true}])
                                  (filter #(= (name ns-name-k) (namespace %))
                                          (keys schemas)))]
                     schemas))
