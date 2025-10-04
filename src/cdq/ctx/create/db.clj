(ns cdq.ctx.create.db
  (:require [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.db.schema :as schema]
            [cdq.db.schema-fn-map :as schema-fn-map]
            [cdq.db.schemas :as schemas]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [gdl.malli :as m]
            [gdl.utils :as utils]))

(defn- save!
  [{:keys [db/data db/file]}]
  (let [data (->> (vals data)
                  (sort-by property/type)
                  (map utils/recur-sort-map)
                  doall)]
    (.start
     (Thread.
      (fn []
        (binding [*print-level* nil]
          (->> data
               pprint/pprint
               with-out-str
               (spit file))))))))

(defrecord Schemas []
  schemas/Schemas
  (build-values [schemas property db]
    (utils/apply-kvs property
                     (fn [k v]
                       (try (schema/create-value (get schemas k) v db)
                            (catch Throwable t
                              (throw (ex-info " " {:k k :v v} t)))))))

  (default-value [schemas k]
    (let [schema (get schemas k)]
      (cond
       (#{:s/map} (schema 0)) {}
       :else nil)))

  (validate [schemas k value]
    (-> (get schemas k)
        (schema/malli-form schemas)
        m/schema
        (m/validate-humanize value)))

  (create-map-schema [schemas ks]
    (m/create-map-schema ks (fn [k]
                              (schema/malli-form (get schemas k) schemas)))))

(defrecord DB []
  db/DB
  (property-types [{:keys [db/schemas]}]
    (filter #(= "properties" (namespace %)) (keys schemas)))

  (update! [{:keys [db/data db/schemas]
             :as this}
            {:keys [property/id] :as property}]
    (assert (contains? property :property/id))
    (assert (contains? data id))
    (schemas/validate schemas (property/type property) property)
    (let [new-db (update this :db/data assoc id property)]
      (save! new-db)
      new-db))

  (delete! [{:keys [db/data] :as this} property-id]
    (assert (contains? data property-id))
    (let [new-db (update this :db/data dissoc property-id)]
      (save! new-db)
      new-db))

  (get-raw [{:keys [db/data]} property-id]
    {:pre [(contains? data property-id)]}
    (get data property-id))

  (all-raw [{:keys [db/data]} property-type]
    (->> (vals data)
         (filter #(= property-type (property/type %)))))

  (build
    [{:keys [db/schemas]
      :as this}
     property-id]
    (schemas/build-values schemas
                          (db/get-raw this property-id)
                          this))

  (build-all
    [{:keys [db/schemas]
      :as this}
     property-type]
    (map #(schemas/build-values schemas % this)
         (db/all-raw this property-type))))

(defn- create-db
  [{:keys [schemas
           properties
           schema-fn-map]}]
  (let [schemas (update-vals (-> schemas io/resource slurp edn/read-string)
                             (fn [[k :as schema]]
                               (with-meta schema (get schema-fn-map k))))
        schemas (map->Schemas schemas)
        properties-file (io/resource properties)
        properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (doseq [property properties]
      (schemas/validate schemas (property/type property) property))
    (merge (map->DB {})
           {:db/data (zipmap (map :property/id properties) properties)
            :db/file properties-file
            :db/schemas schemas})))

(defn do! [ctx]
  (assoc ctx :ctx/db (create-db {:schemas "schema.edn"
                                 :properties "properties.edn"
                                 :schema-fn-map schema-fn-map/fn-map})))
