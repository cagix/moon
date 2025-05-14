(ns cdq.impl.db
  (:require [cdq.db :as db]
            [cdq.db.schema :as schema]
            [cdq.db.property :as property]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

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

(defn- validate-properties! [properties schemas]
  (assert (or (empty? properties)
              (apply distinct? (map :property/id properties))))
  (run! #(schema/validate! schemas (property/type %) %) properties))

#_(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file ; => this is texture ... convert that key itself only?!
   :sub-image-bounds})

(defn- build* [schemas property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema/schema-of schemas k)
                                 (catch Throwable _t
                                   #_(swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build* schemas v)
                         v)]
                 (try (schema/edn->value schema v)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

(defn- async-write-to-file! [{:keys [data file]}]
  ; TODO validate them again!?
  (->> data
       vals
       (sort-by property/type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! file)))

(defrecord DB [data file schemas]
  db/DB
  (update [this {:keys [property/id] :as property}]
    (assert (contains? property :property/id))
    (assert (contains? data id))
    (schema/validate! schemas (property/type property) property)
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
    (build* schemas (db/get-raw this property-id)))

  (build-all [this property-type]
    (map #(build* schemas %) (db/all-raw this property-type))))

(defn create []
  (let [schemas (-> "schema.edn" io/resource slurp edn/read-string)
        properties-file (io/resource "properties.edn")
        properties (-> properties-file slurp edn/read-string)]
    (validate-properties! properties schemas)
    (map->DB {:data (zipmap (map :property/id properties) properties)
              :file properties-file
              :schemas schemas})))
