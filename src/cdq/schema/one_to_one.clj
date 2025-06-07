(ns cdq.schema.one-to-one
  (:require [cdq.db :as db]
            [cdq.schema :as schema]
            [cdq.property :as property]))

(defmethod schema/malli-form :s/one-to-one [[_ property-type] _schemas]
  [:qualified-keyword {:namespace (property/type->id-namespace property-type)}])

(defmethod schema/edn->value :s/one-to-one [_ property-id {:keys [ctx/db] :as ctx}]
  (db/build db property-id ctx))
