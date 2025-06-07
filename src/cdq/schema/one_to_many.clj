(ns cdq.schema.one-to-many
  (:require [cdq.db :as db]
            [cdq.schema :as schema]
            [cdq.property :as property]))

(defmethod schema/malli-form :s/one-to-many [[_ property-type] _schemas]
  [:set [:qualified-keyword {:namespace (property/type->id-namespace property-type)}]])

(defmethod schema/edn->value :s/one-to-many [_ property-ids {:keys [ctx/db] :as ctx}]
  (set (map #(db/build db % ctx) property-ids)))
