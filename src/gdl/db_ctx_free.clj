(ns gdl.db-ctx-free
  (:refer-clojure :exclude [update])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.schema :as schema]
            [gdl.property :as property]
            [gdl.utils :refer [recur-sort-map async-pprint-spit! safe-get]]))

(defn create [{:keys [schema properties]}]
  (let [properties-file (io/resource properties)
        schemas (-> schema io/resource slurp edn/read-string)
        properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! (partial schema/validate! schemas) properties)
    {:db-data (zipmap (map :property/id properties) properties)
     :properties-file properties-file
     :schemas schemas}))

(defn schema-of [{:keys [schemas]} k]
  (schema/schema-of schemas k))

(defn property-types [{:keys [schemas]}]
  (schema/property-types schemas))

(defn async-write-to-file! [{:keys [db-data properties-file]}]
  (->> db-data
       vals
       (sort-by property/type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! properties-file)))

(defn update [{:keys [db-data schemas]} {:keys [property/id] :as property}]
  {:pre [(contains? property :property/id)
         (contains? db-data id)]}
  (schema/validate! schemas property)
  (assoc db-data id property))

(defn delete [{:keys [db-data]} property-id]
  {:pre [(contains? db-data property-id)]}
  (dissoc db-data property-id))

(defn get-raw [{:keys [db-data]} id]
  (safe-get db-data id))

(defn all-raw [{:keys [db-data]} type]
  (->> (vals db-data)
       (filter #(= type (property/type %)))))

; <- fetch ... ? & fetch-raw ? or fetch&build?!
; or just 'get' ... actually ....
; that textures/relationships get cnverted is normal
(defn build [{:keys [schemas] :as db} id]
  (schema/build schemas (get-raw db id)))

(defn build-all [{:keys [schemas] :as db} property-type]
  (map (partial schema/build schemas)
       (all-raw db property-type)))
