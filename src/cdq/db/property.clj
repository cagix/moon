(ns cdq.db.property
  "A property in the database.

  The type is implicit in the `:property/id`.

  For example:

  `:property/id :creatures/toad-horned`

  => type is `:properties/creatures`."
  (:refer-clojure :exclude [type]))

(defn type [{:keys [property/id]}]
  (keyword "properties" (namespace id)))

(defn type->id-namespace [property-type]
  (keyword (name property-type)))
