(ns cdq.db
  (:require [cdq.animation :as animation]
            [cdq.ctx :as ctx]
            [cdq.db.schema :as schema]
            [cdq.db.property :as property]
            [cdq.graphics :as graphics]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]))

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
             pprint
             with-out-str
             (spit file)))))))

(defn- validate-properties! [properties schemas]
  (assert (or (empty? properties)
              (apply distinct? (map :property/id properties))))
  (run! #(schema/validate! schemas (property/type %) %) properties))

(defmulti edn->value (fn [schema v _db]
                       (when schema  ; undefined-data-ks
                         (schema/type schema))))

(defmethod edn->value :default [_schema v _db]
  v)

#_(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file ; => this is texture ... convert that key itself only?!
   :sub-image-bounds})

(defn- build* [{:keys [schemas] :as db} property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema/schema-of schemas k)
                                 (catch Throwable _t
                                   #_(swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build* db v)
                         v)]
                 (try (edn->value schema v db)
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

(defprotocol DB
  (update [_ property])
  (delete [_ property-id])
  (save! [_])
  (get-raw [_ property-id])
  (all-raw [_ property-type])
  (build [_ property-id])
  (build-all [_ property-type]))

(defrecord EDNDB [data file schemas]
  DB
  (update [this {:keys [property/id] :as property}]
    (assert (contains? property :property/id))
    (assert (contains? data id))
    (schema/validate! schemas (property/type property) property)
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
    (build* this (get-raw this property-id)))

  (build-all [this property-type]
    (map #(build* this %) (all-raw this property-type))))

(defn create [] ; TODO pass args - ...
  (let [schemas (-> "schema.edn" io/resource slurp edn/read-string)
        properties-file (io/resource "properties.edn")
        properties (-> properties-file slurp edn/read-string)]
    (validate-properties! properties schemas)
    (map->EDNDB {:data (zipmap (map :property/id properties) properties)
                 :file properties-file
                 :schemas schemas})))

(defn- edn->sprite [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (graphics/from-sheet ctx/graphics
                           (graphics/sprite-sheet ctx/graphics (ctx/assets file) tilew tileh)
                           [(int (/ sprite-x tilew))
                            (int (/ sprite-y tileh))]))
    (graphics/sprite ctx/graphics (ctx/assets file))))

(defmethod edn->value :s/image [_ edn _db]
  (edn->sprite edn))

(defmethod edn->value :s/animation [_ {:keys [frames frame-duration looping?]} _db]
  (animation/create (map edn->sprite frames)
                    :frame-duration frame-duration
                    :looping? looping?))

(defmethod edn->value :s/one-to-one [_ property-id db]
  (build db property-id))

(defmethod edn->value :s/one-to-many [_ property-ids db]
  (set (map #(build db %) property-ids)))
