(ns component.db
  (:refer-clojure :exclude [get])
  (:require [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [component.property :as property]
            [component.schema :as schema]
            [utils.core :refer [safe-get]]))

(comment

 (defn- migrate [property-type migrate-fn]
   (doseq [id (map :property/id (all-raw property-type))]
     (println id)
     (alter-var-root #'db update id migrate-fn))
   (async-write-to-file!))

 (migrate :properties/creatures
          (fn [{:keys [entity/stats] :as creature}]
            (-> creature
                (dissoc :entity/stats)
                (merge stats))))

 )

(declare db
         ^:private edn-file)

(defn load! [file]
  (let [file (clojure.java.io/resource file) ; load here and not in threading macro so #'edn-file correct (tests?!)
        properties (-> file slurp edn/read-string)]
    (assert (apply distinct? (map :property/id properties)))
    (run! property/validate! properties)
    (.bindRoot #'db (zipmap (map :property/id properties) properties))
    (.bindRoot #'edn-file file)))

(defn- async-pprint-spit! [properties]
  (.start
   (Thread.
    (fn []
      (binding [*print-level* nil]
        (->> properties
             pprint
             with-out-str
             (spit edn-file)))))))

(defn- recur-sort-map [m]
  (into (sorted-map)
        (zipmap (keys m)
                (map #(if (map? %)
                        (recur-sort-map %)
                        %)
                     (vals m)))))

(defn- async-write-to-file! []
  (->> db
       vals
       (sort-by property/type)
       (map recur-sort-map)
       doall
       async-pprint-spit!))

(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file
   :sub-image-bounds})

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (clojure.core/get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

(defmulti edn->value (fn [schema v]
                       (when schema  ; undefined-data-ks
                         (schema/type schema))))
(defmethod edn->value :default [_schema v] v)

(defn- build [property]
  (apply-kvs property
             (fn [k v]
               (try (edn->value (try (schema/of k)
                                     (catch Throwable _t
                                       (swap! undefined-data-ks conj k)
                                       nil))
                                (if (map? v)
                                  (build v)
                                  v))
                    (catch Throwable t
                      (throw (ex-info " " {:k k :v v} t)))))))

(defn get [id]
  (build (safe-get db id)))

(defn all-raw [type]
  (->> (vals db)
       (filter #(= type (property/type %)))))

(defn all [type]
  (map build (all-raw type)))

(defn update! [{:keys [property/id] :as property}]
  {:pre [(contains? property :property/id)
         (contains? db id)]}
  (property/validate! property)
  (alter-var-root #'db assoc id property)
  (async-write-to-file!))

(defn delete! [property-id]
  {:pre [(contains? db property-id)]}
  (alter-var-root #'db dissoc property-id)
  (async-write-to-file!))

(defmethod edn->value :s/one-to-one [_ property-id]
  (get property-id))

(defmethod edn->value :s/one-to-many [_ property-ids]
  (map get property-ids))

(defmethod schema/form :s/one-to-one [[_ property-type]]
  [:qualified-keyword {:namespace (property/type->id-namespace property-type)}])

(defmethod schema/form :s/one-to-many [[_ property-type]]
  [:set [:qualified-keyword {:namespace (property/type->id-namespace property-type)}]])
