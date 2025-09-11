(ns cdq.schema.map
  (:require [cdq.malli :as m]
            [cdq.schema :as schema]))

(defmethod schema/malli-form :s/map [[_ ks] schemas]
  (m/create-map-schema ks (fn [k]
                            (schema/malli-form (get schemas k) schemas))))
