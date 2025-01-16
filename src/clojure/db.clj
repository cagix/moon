(ns clojure.db
  (:refer-clojure :exclude [update])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.utils :refer [safe-get]]
            [clojure.schema :as schema]
            [clojure.property :as property]))

(defn- recur-sort-map [m]
  (into (sorted-map)
        (zipmap (keys m)
                (map #(if (map? %)
                        (recur-sort-map %)
                        %)
                     (vals m)))))

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
             pprint
             with-out-str
             (spit file)))))))

(defn async-write-to-file! [{:keys [db/data db/properties-file]}]
  ; TODO validate them again!?
  (->> data
       vals
       (sort-by property/type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! properties-file)))

(defn update [{:keys [db/data] :as db}
              {:keys [property/id] :as property}
              schemas]
  {:pre [(contains? property :property/id)
         (contains? data id)]}
  (schema/validate! schemas (property/type property) property)
  (clojure.core/update db :db/data assoc id property)) ; assoc-in ?

(defn delete [{:keys [db/data] :as db} property-id]
  {:pre [(contains? data property-id)]}
  (clojure.core/update db dissoc :db/data property-id)) ; dissoc-in ?

(defn get-raw [{:keys [db/data]} id]
  (safe-get data id))

(defn all-raw [{:keys [db/data]} type]
  (->> (vals data)
       (filter #(= type (property/type %)))))

(defmulti edn->value (fn [schema v db _c]
                       (when schema  ; undefined-data-ks
                         (schema/type schema))))

(defmethod edn->value :default [_schema v db _c]
  v)

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
                 (try (edn->value schema v db c)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

; <- fetch ... ? & fetch-raw ? or fetch&build?!
; or just 'get' ... actually ....
; that textures/relationships get cnverted is normal
(defn build [db id c]
  (build* c db (get-raw db id)))

(defn build-all [db property-type c]
  (map (partial build* c db)
       (all-raw db property-type)))
