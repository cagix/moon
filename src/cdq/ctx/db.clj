(ns cdq.ctx.db
  (:require [cdq.schemas :as schemas]
            [cdq.property :as property]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn- validate-property [schemas property]
  (schemas/validate schemas (property/type property) property))

(defn- validate-properties! [schemas properties]
  (assert (or (empty? properties)
              (apply distinct? (map :property/id properties))))
  (doseq [property properties]
    (validate-property schemas property)))

(defn create
  [{:keys [schemas
           properties]}]
  (let [schemas (-> schemas io/resource slurp edn/read-string)
        properties-file (io/resource properties)
        properties (-> properties-file slurp edn/read-string)]
    (validate-properties! schemas properties)
    {:data (zipmap (map :property/id properties) properties)
     :file properties-file
     :schemas schemas}))

(defn save-vals! [data-vals file]
  (->> data-vals
       (sort-by property/type)
       (map utils/recur-sort-map)
       doall
       (utils/async-pprint-spit! file)))

(defn- save! [{:keys [data file]}]
  ; TODO validate them again!?
  (save-vals! (vals data)
              file))

(defn property-types [{:keys [schemas]}]
  (filter #(= "properties" (namespace %)) (keys schemas)))

(defn update!
  [{:keys [data schemas]
    :as this}
   {:keys [property/id] :as property}]
  (assert (contains? property :property/id))
  (assert (contains? data id))
  (validate-property schemas property)
  (let [new-db (update this :data assoc id property)]
    (save! new-db)
    new-db))

(defn delete!
  [{:keys [data]
    :as this}
   property-id]
  (assert (contains? data property-id))
  (let [new-db (update this :data dissoc property-id)]
    (save! new-db)
    new-db))

(defn get-raw [{:keys [data]} property-id]
  (utils/safe-get data property-id))

(defn all-raw [{:keys [data]} property-type]
  (->> (vals data)
       (filter #(= property-type (property/type %)))))

(defn build
  [{:keys [schemas]
    :as this}
   property-id]
  (schemas/build-values schemas
                        (get-raw this property-id)
                        this))

(defn build-all
  [{:keys [schemas]
    :as this}
   property-type]
  (map #(schemas/build-values schemas % this)
       (all-raw this property-type)))
