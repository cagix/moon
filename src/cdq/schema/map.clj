(ns cdq.schema.map
  (:require [cdq.schema :as schema]
            [cdq.malli :as m]))

(defmethod schema/malli-form :s/map [[_ ks] schemas]
  (m/create-map-schema ks (fn [k]
                            (schema/malli-form (get schemas k) schemas))))
