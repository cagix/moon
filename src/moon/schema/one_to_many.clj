(ns moon.schema.one-to-many
  (:require [moon.db :as db]
            [moon.property :as property]
            [moon.schema :as schema]))

(defmethod schema/edn->value :s/one-to-many [_ property-ids]
  (set (map db/get property-ids)))

(defmethod schema/form :s/one-to-many [[_ property-type]]
  [:set [:qualified-keyword {:namespace (property/type->id-namespace property-type)}]])
