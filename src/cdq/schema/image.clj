(ns cdq.schema.image
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/image [_ schemas]
  (schema/malli-form [:s/map [:image/file
                              [:image/bounds {:optional true}]]]
                     schemas))
