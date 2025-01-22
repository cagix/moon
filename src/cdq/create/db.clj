(ns cdq.create.db
  (:require [cdq.db :as db]
            [cdq.schema :as schema]
            [cdq.property :as property]
            [cdq.utils :refer [safe-get]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

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

(defn build* [{:keys [cdq/schemas] :as c} db property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema/schema-of schemas k)
                                 (catch Throwable _t
                                   #_(swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build* c db v)
                         v)]
                 (try (schema/edn->value schema v db c)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

(defrecord DB []
  db/DB
  (get-raw [{:keys [db/data]} id]
    (safe-get data id))

  (all-raw [{:keys [db/data]} property-type]
    (->> (vals data)
         (filter #(= property-type (property/type %)))))

  (build [this id context]
    (build* context this (db/get-raw this id)))

  (build-all [this property-type context]
    (map (partial build* context this)
         (db/all-raw this property-type))))

(defn create [{:keys [cdq/schemas]}]
  (let [properties-file (io/resource "properties.edn")
        properties (-> properties-file slurp edn/read-string)]
    (validate-properties! properties schemas)
    (map->DB {:db/data (zipmap (map :property/id properties) properties)
              :db/properties-file properties-file})))
