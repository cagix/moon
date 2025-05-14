(ns cdq.impl.db
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.schemas :as schemas]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

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
  db/DB
  (update [this {:keys [property/id] :as property}]
    (assert (contains? property :property/id))
    (assert (contains? data id))
    (schemas/validate! ctx/schemas (property/type property) property)
    (update this :data assoc id property))

  (delete [this property-id]
    (assert (contains? data property-id))
    (update this :data dissoc property-id))

  (save! [this]
    (async-write-to-file! this))

  (get-raw [_ property-id]
    (utils/safe-get data property-id))

  (all-raw [_ property-type]
    (->> (vals data)
         (filter #(= property-type (property/type %)))))

  (build [this property-id]
    (schemas/transform ctx/schemas (db/get-raw this property-id)))

  (build-all [this property-type]
    (map #(schemas/transform ctx/schemas %) (db/all-raw this property-type))))

(defn create [path]
  (let [properties-file (io/resource path) ; TODO required from existing?
        properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! #(schemas/validate! ctx/schemas (property/type %) %) properties)
    (map->DB {:data (zipmap (map :property/id properties) properties)
              :file properties-file})))
