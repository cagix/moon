(ns cdq.db
  (:require [cdq.graphics :as graphics]
            [cdq.graphics.animation :as animation]
            [cdq.schema :as schema]
            [cdq.property :as property]
            [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
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

(declare ^:private -data
         ^:private -properties-file
         ^:private -schemas)

(defn- validate-properties! [properties]
  (assert (or (empty? properties)
              (apply distinct? (map :property/id properties))))
  (run! #(schema/validate! -schemas (property/type %) %) properties))

(defn create! []
  (let [schemas (-> "schema.edn" io/resource slurp edn/read-string)
        properties-file (io/resource "properties.edn")
        properties (-> properties-file slurp edn/read-string)]
    (.bindRoot #'-schemas schemas)
    (validate-properties! properties)
    (.bindRoot #'-data (zipmap (map :property/id properties) properties))
    (.bindRoot #'-properties-file properties-file)))

#_(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file ; => this is texture ... convert that key itself only?!
   :sub-image-bounds})

(defn- build* [property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema/schema-of -schemas k)
                                 (catch Throwable _t
                                   #_(swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build* v)
                         v)]
                 (try (schema/edn->value schema v)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

(defn- async-write-to-file! []
  ; TODO validate them again!?
  (->> -data
       vals
       (sort-by property/type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! -properties-file)))

(defn update! [{:keys [property/id] :as property}]
  {:pre [(contains? property :property/id)
         (contains? -data id)]}
  (schema/validate! -schemas (property/type property) property)
  (alter-var-root #'-data assoc id property)
  (async-write-to-file!))

(defn delete! [property-id]
  {:pre [(contains? -data property-id)]}
  (alter-var-root #'-data dissoc :db/data property-id)
  (async-write-to-file!))

(defn get-raw [property-id]
  (utils/safe-get -data property-id))

(defn all-raw [property-type]
  (->> (vals -data)
       (filter #(= property-type (property/type %)))))

(defn build [property-id]
  (build* (get-raw property-id)))

(defn build-all [property-type]
  (map build* (all-raw property-type)))

(defn- edn->sprite [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (graphics/from-sheet (graphics/sprite-sheet file tilew tileh)
                           [(int (/ sprite-x tilew))
                            (int (/ sprite-y tileh))]))
    (graphics/->sprite file)))

(defmethod schema/edn->value :s/image [_ edn]
  (edn->sprite edn))

(defmethod schema/edn->value :s/animation [_ {:keys [frames frame-duration looping?]}]
  (animation/create (map edn->sprite frames)
                    :frame-duration frame-duration
                    :looping? looping?))

(defmethod schema/edn->value :s/one-to-one [_ property-id]
  (build property-id))

(defmethod schema/edn->value :s/one-to-many [_ property-ids]
  (set (map build property-ids)))
