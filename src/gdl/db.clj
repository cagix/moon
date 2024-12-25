(ns gdl.db
  (:refer-clojure :exclude [update])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.schema :as schema]
            [gdl.property :as property]))

(defn create [{:keys [schema properties]}]
  (let [properties-file (io/resource properties)
        schemas (-> schema io/resource slurp edn/read-string)
        properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! (partial schema/validate! schemas) properties)
    {:db/data (zipmap (map :property/id properties) properties)
     :db/properties-file properties-file
     :db/schemas schemas}))

(defn schema-of [{:keys [db/schemas]} k]
  (schema/schema-of schemas k))

(defn property-types [{:keys [db/schemas]}]
  (schema/property-types schemas))

(defn async-write-to-file! [{:keys [db/data db/properties-file]}]
  ; TODO validate them again!?
  (->> data
       vals
       (sort-by property/type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! properties-file)))

(defn update [{:keys [db/data db/schemas] :as db}
              {:keys [property/id] :as property}]
  {:pre [(contains? property :property/id)
         (contains? data id)]}
  (schema/validate! schemas property)
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

(defn build* [c db property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema-of db k)
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
