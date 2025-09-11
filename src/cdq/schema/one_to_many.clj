(ns cdq.schema.one-to-many
  (:require [cdq.db :as db]
            [cdq.schema :as schema]
            [cdq.property :as property]))

(defmethod schema/malli-form :s/one-to-many [[_ property-type] _schemas]
  [:set [:qualified-keyword {:namespace (property/type->id-namespace property-type)}]])

(defmethod schema/create-value :s/one-to-many [_ property-ids db]
  (set (map (partial db/build db) property-ids)))
