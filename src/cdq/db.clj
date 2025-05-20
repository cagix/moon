(ns cdq.db
  (:refer-clojure :exclude [update])
  (:require [cdq.ctx :as ctx]
            [cdq.schema :as schema]
            [cdq.property :as property]
            [cdq.malli :as m]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defprotocol PDB
  (update [_ property]
          "Validates the given property, throws an error if invalid and asserts its id is contained in the database.

          Returns a new database with the property updated.")
  (delete [_ property-id]
          "Asserts if a property with property-id is contained in the database.

          Returns a new database with the property removed.")
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

(defn- validate! [schemas property]
  (m/form->validate (schema/malli-form (get schemas (property/type property))
                                       schemas)
                    property))

(defrecord DB [data file]
  PDB
  (update [this {:keys [property/id] :as property}]
    (assert (contains? property :property/id))
    (assert (contains? data id))
    (validate! ctx/schemas property)
    (clojure.core/update this :data assoc id property))

  (delete [this property-id]
    (assert (contains? data property-id))
    (clojure.core/update this :data dissoc property-id))

  (save! [_]
    ; TODO validate them again!?
    (->> data
         vals
         (sort-by property/type)
         (map utils/recur-sort-map)
         doall
         (utils/async-pprint-spit! file)))

  (get-raw [_ property-id]
    (utils/safe-get data property-id))

  (all-raw [_ property-type]
    (->> (vals data)
         (filter #(= property-type (property/type %)))))

  (build [this property-id]
    (schema/transform ctx/schemas (get-raw this property-id)))

  (build-all [this property-type]
    (map #(schema/transform ctx/schemas %) (all-raw this property-type))))

(defn create [path]
  (let [properties-file (io/resource path) ; TODO required from existing?
        properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! (partial validate! ctx/schemas) properties)
    (map->DB {:data (zipmap (map :property/id properties) properties)
              :file properties-file})))
