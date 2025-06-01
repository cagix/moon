(ns gdl.create.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.db :as db]
            [gdl.malli :as m]
            [gdl.schema :as schema]
            [gdl.schemas :as schemas]
            [gdl.property :as property]
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

(defmulti malli-form (fn [schema _schemas] (schema/type schema)))
(defmethod malli-form :default [schema _schemas] schema)

#_(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file ; => this is texture ... convert that key itself only?!
   :sub-image-bounds})

(defn- transform* [schemas property ctx]
  (utils/apply-kvs property
                   (fn [k v]
                     (let [schema (try (get schemas k)
                                       (catch Throwable _t
                                         #_(swap! undefined-data-ks conj k)
                                         nil))
                           v (if (map? v)
                               (transform* schemas v ctx)
                               v)]
                       (try (schema/edn->value schema v ctx)
                            (catch Throwable t
                              (throw (ex-info " " {:k k :v v} t))))))))

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

  (transform [schemas property ctx]
    (transform* schemas property ctx))

  (map-keys [_ map-schema]
    (m/map-keys (malli-form map-schema data)))

  (optional-keyset [_ map-schema]
    (m/optional-keyset (malli-form map-schema data)))

  (optional-k? [_ map-schema k]
    (m/optional? k (malli-form map-schema data)))

  (k->default-value [_ k]
    (let [schema (gdl.utils/safe-get data k)]
      (cond
       (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

       ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

       :else (m/generate (malli-form schema data)
                         {:size 3})))))

(defn- create-db [{:keys [schemas
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

(defn do! [{:keys [ctx/config] :as ctx}]
  (assoc ctx :ctx/db (create-db (:db config))))

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

(defmethod malli-form :s/number  [_ _schemas] number?)
(defmethod malli-form :s/nat-int [_ _schemas] nat-int?)
(defmethod malli-form :s/int     [_ _schemas] int?)
(defmethod malli-form :s/pos     [_ _schemas] pos?)
(defmethod malli-form :s/pos-int [_ _schemas] pos-int?)

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defmethod malli-form :s/one-to-one [[_ property-type] _schemas]
  [:qualified-keyword {:namespace (type->id-namespace property-type)}])

(defmethod malli-form :s/one-to-many [[_ property-type] _schemas]
  [:set [:qualified-keyword {:namespace (type->id-namespace property-type)}]])

(defn- map-schema
  "Can define keys as just keywords or with schema-props like [:foo {:optional true}]."
  [ks k->malli-schema-form]
  (apply vector :map {:closed true}
         (for [k ks
               :let [k? (keyword? k)
                     schema-props (if k? nil (k 1))
                     k (if k? k (k 0))]]
           (do
            (assert (keyword? k))
            (assert (or (nil? schema-props) (map? schema-props)) (pr-str ks))
            [k schema-props (k->malli-schema-form k)]))))

(defn- map-form [ks schemas]
  (map-schema ks (fn [k]
                   (malli-form (get schemas k) schemas))))

(defmethod malli-form :s/map [[_ ks] schemas]
  (map-form ks schemas))

(defmethod malli-form :s/map-optional [[_ ks] schemas]
  (map-form (map (fn [k] [k {:optional true}]) ks)
            schemas))

(defn- namespaced-ks [schemas ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schemas)))

(defmethod malli-form :s/components-ns [[_ ns-name-k] schemas]
  (malli-form [:s/map-optional (namespaced-ks schemas ns-name-k)]
              schemas))

