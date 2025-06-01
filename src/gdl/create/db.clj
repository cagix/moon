(ns gdl.create.db
  (:require [cdq.schema :as schema]
            [cdq.schemas :as schemas]
            [cdq.schemas-impl :as schemas-impl]
            [cdq.property :as property]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.db :as db]
            [gdl.utils :as utils]))

(defn- save! [{:keys [data file]}]
  ; TODO validate them again!?
  (->> data
       vals
       (sort-by property/type)
       (map utils/recur-sort-map)
       doall
       (utils/async-pprint-spit! file)))

(defrecord DB [data file schemas]
  db/Database
  (property-types [_]
    (schemas/property-types schemas))

  (update! [this {:keys [property/id] :as property}]
    (assert (contains? property :property/id))
    (assert (contains? data id))
    (schemas/validate schemas property)
    (let [new-db (update this :data assoc id property)]
      (save! new-db)
      new-db))

  (delete! [this property-id]
    (assert (contains? data property-id))
    (let [new-db (update this :data dissoc property-id)]
      (save! new-db)
      new-db))

  (get-raw [_ property-id]
    (utils/safe-get data property-id))

  (all-raw [_ property-type]
    (->> (vals data)
         (filter #(= property-type (property/type %)))))

  ; why this needs ctx?
  ; or we build the db at start?
  ; need to pass whole ctx at every creature spawn ....
  ; we had some problem with relationships
  ; why we don't just use datomic ?
  (build [this property-id ctx]
    (schemas/transform schemas
                       (db/get-raw this property-id)
                       ctx))

  (build-all [this property-type ctx]
    (map #(schemas/transform schemas % ctx)
         (db/all-raw this property-type))))

(defn- create-db [{:keys [schemas
                      properties]}]
  (let [schemas (schemas-impl/create (utils/io-slurp-edn schemas))
        properties-file (io/resource properties)
        properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! (partial schemas/validate schemas) properties)
    (map->DB {:data (zipmap (map :property/id properties) properties)
              :file properties-file
              :schemas schemas})))

; why do I do transform edn->value in the middle of running game need full ctx graphics/assets
; and db to spawn a creature ?
; how is it related to editor (we dont want to build everything, work with raw data there)
; and how is it related with relationships, why not just add create skill/item work with id?
; what was the reason for this?
; also for the info texts there is too much showing
; => observability -> tiles/context/app-values-tree/stage ui elements/entity see in game
; right click map tree view step by step.

(defmethod schema/edn->value :s/one-to-one [_ property-id {:keys [ctx/db] :as ctx}]
  (db/build db property-id ctx))

(defmethod schema/edn->value :s/one-to-many [_ property-ids {:keys [ctx/db] :as ctx}]
  (set (map #(db/build db % ctx) property-ids)))

(defn do! [{:keys [ctx/config] :as ctx}]
  (assoc ctx :ctx/db (create-db (:db config))))
