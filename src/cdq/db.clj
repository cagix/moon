(ns cdq.db
  (:require [cdq.schemas :as schemas]
            [cdq.property :as property]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.utils]))

(defprotocol PDB
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

(defn- save! [{:keys [data file]}]
  ; TODO validate them again!?
  (->> data
       vals
       (sort-by property/type)
       (map utils/recur-sort-map)
       doall
       (utils/async-pprint-spit! file)))

(defrecord DB [data file schemas]
  PDB
  (update! [this {:keys [property/id] :as property}]
    (assert (contains? property :property/id))
    (assert (contains? data id))
    (schemas/validate schemas property)
    (let [new-db (update this :data assoc id property)]
      (save! new-db)
      new-db))

  (delete! [this property-id]
    (assert (contains? data property-id))
    (let [new-db (update this :data dissoc property-id)]
      (save! new-db)
      new-db))

  (get-raw [_ property-id]
    (gdl.utils/safe-get data property-id))

  (all-raw [_ property-type]
    (->> (vals data)
         (filter #(= property-type (property/type %)))))

  (build [this property-id ctx]
    (schemas/transform schemas
                       (get-raw this property-id)
                       ctx))

  (build-all [this property-type ctx]
    (map #(schemas/transform schemas % ctx)
         (all-raw this property-type))))

(defn create [{:keys [schemas
                      properties]}]
  (let [properties-file (io/resource properties) ; TODO required from existing?
        properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! (partial schemas/validate schemas) properties)
    (map->DB {:data (zipmap (map :property/id properties) properties)
              :file properties-file
              :schemas schemas})))
