(ns cdq.impl.db
  (:require [cdq.db :as db]
            [cdq.schema :as schema]
            [cdq.schemas :as schemas]
            [cdq.malli :as m]
            [cdq.property :as property]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [malli.core]
            [malli.map-schema :as map-schema]))

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

(defn- async-pprint-spit! [file data]
  (.start
   (Thread.
    (fn []
      (binding [*print-level* nil]
        (->> data
             pprint/pprint
             with-out-str
             (spit file)))))))

(defn- recur-sort-map [m]
  (into (sorted-map)
        (zipmap (keys m)
                (map #(if (map? %)
                        (recur-sort-map %)
                        %)
                     (vals m)))))

(defn save-vals! [data-vals file]
  (->> data-vals
       (sort-by property/type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! file)))

(defn- save! [{:keys [data file]}]
  ; TODO validate them again!?
  (save-vals! (vals data)
              file))

(defn- validate-property [schemas property]
  (schemas/validate schemas (property/type property) property))

(defn- validate-properties! [schemas properties]
  (assert (or (empty? properties)
              (apply distinct? (map :property/id properties))))
  (doseq [property properties]
    (validate-property schemas property)))

(defn- convert-fn-map [fn-map]
  (into {} (for [[proto-sym impl-sym] fn-map]
             [proto-sym (requiring-resolve impl-sym)])))

(defrecord Schemas []
  schemas/Schemas
  (build-values [schemas property db]
    (apply-kvs property
               (fn [k v]
                 (try (schema/create-value (get schemas k) v db)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t)))))))

  (default-value [schemas k]
    (let [schema (get schemas k)]
      (cond
       ;(#{:s/one-to-one :s/one-to-many} (get-type schema)) nil
       (#{:s/map} (schema 0)) {} ; cannot have empty for required keys, then no Add Component button
       :else nil
       ;:else (m/generate (schema/malli-form schema schemas) {:size 3})

       )))

  (validate [schemas k value]
    (-> (get schemas k)
        (schema/malli-form schemas)
        malli.core/schema
        (m/validate-humanize value)))

  (create-map-schema [schemas ks]
    (map-schema/create-map-schema ks (fn [k]
                                       (schema/malli-form (get schemas k) schemas)))))

(defrecord DB []
  db/DB
  (property-types [{:keys [schemas]}]
    (filter #(= "properties" (namespace %)) (keys schemas)))

  (update! [{:keys [data schemas]
             :as this}
            {:keys [property/id] :as property}]
    (assert (contains? property :property/id))
    (assert (contains? data id))
    (validate-property schemas property)
    (let [new-db (update this :data assoc id property)]
      (save! new-db)
      new-db))

  (delete! [{:keys [data] :as this} property-id]
    (assert (contains? data property-id))
    (let [new-db (update this :data dissoc property-id)]
      (save! new-db)
      new-db))

  (get-raw [{:keys [data]} property-id]
    {:pre [(contains? data property-id)]}
    (get data property-id))

  (all-raw [{:keys [data]} property-type]
    (->> (vals data)
         (filter #(= property-type (property/type %)))))

  (build
    [{:keys [schemas]
      :as this}
     property-id]
    (schemas/build-values schemas
                          (db/get-raw this property-id)
                          this))

  (build-all
    [{:keys [schemas]
      :as this}
     property-type]
    (map #(schemas/build-values schemas % this)
         (db/all-raw this property-type))))

(defn create
  [{:keys [schemas
           properties]}]
  (let [schema-fn-map (into {} (for [[k fn-map] (-> "schema_fn_map.edn" io/resource slurp edn/read-string)]
                                 [k (convert-fn-map fn-map)]))
        schemas (update-vals (-> schemas io/resource slurp edn/read-string)
                             (fn [[k :as schema]]
                               (with-meta schema (get schema-fn-map k))))
        schemas (map->Schemas schemas)
        properties-file (io/resource properties)
        properties (-> properties-file slurp edn/read-string)]
    (validate-properties! schemas properties)
    (merge (map->DB {})
           {:data (zipmap (map :property/id properties) properties)
            :file properties-file
            :schemas schemas})))
