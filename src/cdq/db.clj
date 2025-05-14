(ns cdq.db
  (:refer-clojure :exclude [update]))

(defprotocol DB
  (update [_ property]
          "Validates the given property and asserts its id is contained in the database.

          Returns a new database with the property updated.")
  (delete [_ property-id]
          "Asserts if the property-id is contained in the database.

          Returns a new database with the property-id removed.")
  (save! [_]
         "Writes the database to disk asynchronously in another thread.")
  (get-raw [_ property-id]
           "Returns the property value without schema based transformations.")
  (all-raw [_ property-type]
           "Returns all properties with type without schema-based transformations.")
  (build [_ property-id]
         "Returns the property with schema-based transformations.")
  (build-all [_ property-type]
             "Returns all properties with type with schema-based transformations."))
