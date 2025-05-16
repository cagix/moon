(ns cdq.db
  (:refer-clojure :exclude [update])
  (:require [cdq.ctx :as ctx]
            [cdq.schema :as schema]
            [cdq.property :as property]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [malli.core :as m]
            [malli.error :as me]))

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

(defn- validate!* [form value]
  (let [schema (m/schema form)]
    (when-not (m/validate schema value)
      (throw (ex-info (str (me/humanize (m/explain schema value)))
                      {:value value
                       :schema (m/form schema)})))))

(comment
 (validate!* [:map {:closed true}
              [:foo pos?]
              [:bar pos?]
              [:baz {:optional true} :some]
              [:boz {:optional false} :some]
              [:asdf {:optional true} :some]]
             {:foo 1
              :bar 2
              :boz :a
              :asdf :b
              :baz :asdf})

 (validate!* [:map {:closed true}
              [:foo pos?]
              [:bar pos?]
              [:baz {:optional true} :some]
              [:boz {:optional false} :some]
              [:asdf {:optional true} :some]]
             {:foo 1
              :bar 2
              :boz :a})

 (validate!* [:map {:closed true}
              [:foo pos?]
              [:bar pos?]
              [:baz {:optional true} :some]
              [:boz {:optional false} :some]
              [:asdf {:optional true} :some]]
             {:bar 2
              :boz :a})
 )

(defn- validate! [property]
  (validate!* (schema/malli-form (get ctx/schemas (property/type property))
                                 ctx/schemas)
              property))

(defn- recur-sort-map [m]
  (into (sorted-map)
        (zipmap (keys m)
                (map #(if (map? %)
                        (recur-sort-map %)
                        %)
                     (vals m)))))

(defn- async-pprint-spit! [file data]
  (.start
   (Thread.
    (fn []
      (binding [*print-level* nil]
        (->> data
             pprint/pprint
             with-out-str
             (spit file)))))))

(defn- async-write-to-file! [{:keys [data file]}]
  ; TODO validate them again!?
  (->> data
       vals
       (sort-by property/type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! file)))

(defrecord DB [data file]
  PDB
  (update [this {:keys [property/id] :as property}]
    (assert (contains? property :property/id))
    (assert (contains? data id))
    (validate! property)
    (clojure.core/update this :data assoc id property))

  (delete [this property-id]
    (assert (contains? data property-id))
    (clojure.core/update this :data dissoc property-id))

  (save! [this]
    (async-write-to-file! this))

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
    (run! validate! properties)
    (map->DB {:data (zipmap (map :property/id properties) properties)
              :file properties-file})))
