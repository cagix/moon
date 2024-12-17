(ns gdl.db-ctx-free
  (:refer-clojure :exclude [update])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.graphics.animation :as animation]
            [gdl.graphics.sprite :as sprite]
            [gdl.schema :as schema]
            [gdl.property :as property]
            [gdl.malli :as m]
            [gdl.utils :refer [recur-sort-map async-pprint-spit! safe-get apply-kvs defmethods]]))

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
  (->> data
       vals
       (sort-by property/type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! properties-file)))

(defn update [{:keys [db/data db/schemas]} {:keys [property/id] :as property}]
  {:pre [(contains? property :property/id)
         (contains? data id)]}
  (schema/validate! schemas property)
  (assoc data id property))

(defn delete [{:keys [db/data]} property-id]
  {:pre [(contains? data property-id)]}
  (dissoc data property-id))

(defn get-raw [{:keys [db/data]} id]
  (safe-get data id))

(defn all-raw [{:keys [db/data]} type]
  (->> (vals data)
       (filter #(= type (property/type %)))))

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

(defn build* [db property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema-of db k)
                                 (catch Throwable _t
                                   #_(swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build* db v)
                         v)]
                 (try (edn->value schema v db)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

; <- fetch ... ? & fetch-raw ? or fetch&build?!
; or just 'get' ... actually ....
; that textures/relationships get cnverted is normal
(defn build [db id]
  (build* db (get-raw db id)))

(defn build-all [db property-type]
  (map (partial build* db)
       (all-raw db property-type)))

; TODO do we care in here about malli-form ?! - where used? - hide inside 'schemas' ? or schemas/validation

(defmethod schema/malli-form :s/val-max [_ _schemas] m/val-max-schema)
(defmethod schema/malli-form :s/number  [_ _schemas] m/number-schema)
(defmethod schema/malli-form :s/nat-int [_ _schemas] m/nat-int-schema)
(defmethod schema/malli-form :s/int     [_ _schemas] m/int-schema)
(defmethod schema/malli-form :s/pos     [_ _schemas] m/pos-schema)
(defmethod schema/malli-form :s/pos-int [_ _schemas] m/pos-int-schema)
(defmethod schema/malli-form :s/sound   [_ _schemas] m/string-schema)

(defn- edn->sprite [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (sprite/from-sheet (sprite/sheet file tilew tileh)
                         [(int (/ sprite-x tilew))
                          (int (/ sprite-y tileh))]))
    (sprite/create file)))

(defmethods :s/image
  (schema/malli-form  [_ _schemas]
    m/image-schema)

  (edn->value [_ edn _db]
    (edn->sprite edn)))

(defmethods :s/animation
  (schema/malli-form [_ _schemas]
    m/animation-schema)

  (edn->value [_ {:keys [frames frame-duration looping?]} _db]
    (animation/create (map edn->sprite frames)
                      :frame-duration frame-duration
                      :looping? looping?)))

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defmethods :s/one-to-one
  (schema/malli-form [[_ property-type] _schemas]
    (m/qualified-keyword-schema (type->id-namespace property-type)))
  (edn->value [_ property-id db]
    (build db property-id)))

(defmethods :s/one-to-many
  (schema/malli-form [[_ property-type] _schemas]
    (m/set-schema (m/qualified-keyword-schema (type->id-namespace property-type))))
  (edn->value [_ property-ids db]
    (set (map (partial build db) property-ids))))

(defn- map-form [ks schemas]
  (m/map-schema ks (fn [k]
                     (schema/malli-form (schema/schema-of schemas k)
                                        schemas))))
; TODO schema/validate comes to this...
; but db-data is not yet existing?

(defmethod schema/malli-form :s/map [[_ ks] schemas]
  (map-form ks schemas))

(defmethod schema/malli-form :s/map-optional [[_ ks] schemas]
  (map-form (map (fn [k] [k {:optional true}]) ks)
            schemas))

(defn- namespaced-ks [schemas ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schemas)))

(defmethod schema/malli-form :s/components-ns [[_ ns-name-k] schemas]
  (schema/malli-form [:s/map-optional (namespaced-ks schemas ns-name-k)]
                     schemas))
