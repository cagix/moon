(ns cdq.schema.val-max
  (:require [cdq.create.db :refer [malli-form]]
            [cdq.val-max :as val-max]))

(defmethod malli-form :s/val-max [_ _schemas] val-max/schema)
