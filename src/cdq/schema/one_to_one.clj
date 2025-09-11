(ns cdq.schema.one-to-one
  (:require [cdq.db :as db]
            [cdq.schema :as schema]
            [cdq.property :as property]))

(defmethod schema/create-value :s/one-to-one [_ property-id db]
  (db/build db property-id))

(defmethod schema/malli-form :s/one-to-one [[_ property-type] _schemas]
  [:qualified-keyword {:namespace (property/type->id-namespace property-type)}])
