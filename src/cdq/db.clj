(ns cdq.db)

(defprotocol Database
  (property-types [_])

  (update! [_ property]
          "Validates the given property, throws an error if invalid and asserts its id is contained in the database.

          Writes the database to disk asynchronously.

          Returns a new database with the property updated.")

  (delete! [_ property-id]
          "Asserts if a property with property-id is contained in the database.

          Writes the database to disk asynchronously.

          Returns a new database with the property removed.")

  (get-raw [_ property-id]
           "Returns the property value without schema based transformations.")

  (all-raw [_ property-type]
           "Returns all properties with type without schema-based transformations.")

  (build [_ property-id ctx]
         "Returns the property with schema-based transformations.")

  (build-all [_ property-type ctx]
             "Returns all properties with type with schema-based transformations."))
