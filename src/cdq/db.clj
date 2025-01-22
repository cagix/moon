(ns cdq.db
  (:refer-clojure :exclude [update])
  (:require [clojure.pprint :refer [pprint]]
            [cdq.schema :as schema]
            [cdq.property :as property]))

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

(defprotocol DB
  (get-raw [_ id])
  (all-raw [_ property-type])
  (build [_ id context])
  (build-all [_ property-type context]))
