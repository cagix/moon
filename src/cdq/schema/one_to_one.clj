(ns cdq.schema.one-to-one
  (:require [cdq.schema :as schema]
            [cdq.property :as property]))

(defmethod schema/malli-form :s/one-to-one [[_ property-type] _schemas]
  [:qualified-keyword {:namespace (property/type->id-namespace property-type)}])
