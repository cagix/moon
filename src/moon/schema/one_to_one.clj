(ns moon.schema.one-to-one
  (:require [moon.db :as db]
            [moon.property :as property]
            [moon.schema :as schema]))

(defmethod schema/edn->value :s/one-to-one [_ property-id]
  (db/get property-id))

(defmethod schema/form :s/one-to-one [[_ property-type]]
  [:qualified-keyword {:namespace (property/type->id-namespace property-type)}])
