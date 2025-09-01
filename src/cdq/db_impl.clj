(ns cdq.db-impl
  (:require [cdq.ctx.db :as db]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cdq.schema :as schema]
            [cdq.schemas :as schemas]
            [cdq.property :as property]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]
            [cdq.malli :as m]))

(defmulti fetch* (fn [schema _v _db]
                   (schema/type schema)))

(defmethod fetch* :default [_schema v _db]
  v)

(defmethod fetch* :s/one-to-many [_ property-ids db]
  (set (map (partial db/build db) property-ids)))

(defmethod fetch* :s/one-to-one [_ property-id db]
  (db/build db property-id))

(defn- fetch-relationships [schemas property db]
  (utils/apply-kvs property
                   (fn [k v]
                     (let [schema (get schemas k)
                           v (if (map? v)
                               (fetch-relationships schemas v db)
                               v)]
                       (try (fetch* schema v db)
                            (catch Throwable t
                              (throw (ex-info " " {:k k :v v} t))))))))

(defn save-vals! [data-vals file]
  (->> data-vals
       (sort-by property/type)
       (map utils/recur-sort-map)
       doall
       (utils/async-pprint-spit! file)))

(defn- save! [{:keys [data file]}]
  ; TODO validate them again!?
  (save-vals! (vals data)
              file))

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

  (build [this property-id]
    (fetch-relationships schemas
                         (db/get-raw this property-id)
                         this))

  (build-all [this property-type]
    (map #(fetch-relationships schemas % this)
         (db/all-raw this property-type))))

(defmulti malli-form (fn [schema _schemas] (schema/type schema)))
(defmethod malli-form :default [schema _schemas] schema)

(defmethod malli-form :s/int     [_ _schemas] int?)
(defmethod malli-form :s/nat-int [_ _schemas] nat-int?)
(defmethod malli-form :s/pos     [_ _schemas] pos?)
(defmethod malli-form :s/pos-int [_ _schemas] pos-int?)
(defmethod malli-form :s/number  [_ _schemas] number?)

(defmethod malli-form :s/one-to-many [[_ property-type] _schemas]
  [:set [:qualified-keyword {:namespace (property/type->id-namespace property-type)}]])

(defmethod malli-form :s/one-to-one [[_ property-type] _schemas]
  [:qualified-keyword {:namespace (property/type->id-namespace property-type)}])

(defmethod malli-form :s/sound [_ _schemas]
  :string)

(defmethod malli-form :s/val-max [_ _schemas]
  val-max/schema)

(defmethod malli-form :s/map [[_ ks] schemas]
  (m/create-map-schema ks (fn [k]
                            (malli-form (get schemas k) schemas))))

(defmethod malli-form :s/map-optional [[_ ks] schemas]
  (malli-form [:s/map (map (fn [k] [k {:optional true}]) ks)]
              schemas))

(defmethod malli-form :s/components-ns [[_ ns-name-k] schemas]
  (malli-form [:s/map-optional (filter #(= (name ns-name-k) (namespace %))
                                       (keys schemas))]
              schemas))

(deftype Schemas [data]
  clojure.lang.ILookup
  (valAt [_ key]
    (utils/safe-get data key))

  schemas/Schemas
  (property-types [_]
    (filter #(= "properties" (namespace %)) (keys data)))

  (validate [_ property]
    (m/form->validate (malli-form (get data (property/type property))
                                  data)
                      property))

  (map-keys [_ map-schema]
    (m/map-keys (malli-form map-schema data)))

  (optional-keyset [_ map-schema]
    (m/optional-keyset (malli-form map-schema data)))

  (optional-k? [_ map-schema k]
    (m/optional? k (malli-form map-schema data)))

  (k->default-value [_ k]
    (let [schema (cdq.utils/safe-get data k)]
      (cond
       (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

       ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

       :else (m/generate (malli-form schema data)
                         {:size 3})))))

(defn create
  [{:keys [schemas
           properties]}]
  (let [schemas (->Schemas (utils/io-slurp-edn schemas))
        properties-file (io/resource properties)
        properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! (partial schemas/validate schemas) properties)
    (map->DB {:data (zipmap (map :property/id properties) properties)
              :file properties-file
              :schemas schemas})))
