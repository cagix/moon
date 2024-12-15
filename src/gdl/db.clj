(ns gdl.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.graphics.animation :as animation]
            [gdl.graphics.sprite :as sprite]
            [gdl.malli :as m]
            [gdl.utils :refer [safe-get recur-sort-map apply-kvs async-pprint-spit! defmethods]]))

(defn schema-type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defn property-type [{:keys [property/id]}]
  (keyword "properties" (namespace id)))

(declare ^:private schemas)

(defn property-types []
  (filter #(= "properties" (namespace %))
          (keys schemas)))

(defn schema-of [k]
  (safe-get schemas k))

(defmulti malli-form schema-type)
(defmethod malli-form :default [schema] schema)

(declare build)

(defmulti edn->value (fn [schema v]
                       (when schema  ; undefined-data-ks
                         (schema-type schema))))
(defmethod edn->value :default [_schema v] v)

(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file
   :sub-image-bounds})

(defn- build* [property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema-of k)
                                 (catch Throwable _t
                                   (swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build* v)
                         v)]
                 (try (edn->value schema v)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

(defn validate! [property]
  (m/validate! (-> property
                   property-type
                   schema-of
                   malli-form)
               property))

(declare properties-file
         db-data)

(defn- async-write-to-file! []
  (->> db-data
       vals
       (sort-by property-type)
       (map recur-sort-map)
       doall
       (async-pprint-spit! properties-file)))

(defn update! [{:keys [property/id] :as property}]
  {:pre [(contains? property :property/id)
         (contains? db-data id)]}
  (validate! property)
  (alter-var-root #'db-data assoc id property)
  (async-write-to-file!))

(defn delete! [property-id]
  {:pre [(contains? db-data property-id)]}
  (alter-var-root #'db-data dissoc property-id)
  (async-write-to-file!))

(defn get-raw [id]
  (safe-get db-data id))

(defn all-raw [type]
  (->> (vals db-data)
       (filter #(= type (property-type %)))))

(defn build [id]
  (build* (get-raw id)))

(defn build-all [type]
  (map build* (all-raw type)))

(defn migrate [property-type update-fn]
  (doseq [id (map :property/id (all-raw property-type))]
    (println id)
    (alter-var-root #'db-data update id update-fn))
  (async-write-to-file!))

(defn setup [{:keys [schema properties]}]
  (def schemas (-> schema io/resource slurp edn/read-string))
  (def properties-file (io/resource properties))
  (let [properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! validate! properties)
    (def db-data (zipmap (map :property/id properties) properties))))

(defmethod malli-form :s/val-max [_] m/val-max-schema)
(defmethod malli-form :s/number  [_] m/number-schema)
(defmethod malli-form :s/nat-int [_] m/nat-int-schema)
(defmethod malli-form :s/int     [_] m/int-schema)
(defmethod malli-form :s/pos     [_] m/pos-schema)
(defmethod malli-form :s/pos-int [_] m/pos-int-schema)
(defmethod malli-form :s/sound   [_] m/string-schema)

(defn- edn->sprite [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (sprite/from-sheet (sprite/sheet file tilew tileh)
                         [(int (/ sprite-x tilew))
                          (int (/ sprite-y tileh))]))
    (sprite/create file)))

(defmethods :s/image
  (malli-form  [_]
    m/image-schema)

  (edn->value [_ edn]
    (edn->sprite edn)))

(defmethods :s/animation
  (malli-form [_]
    m/animation-schema)

  (edn->value [_ {:keys [frames frame-duration looping?]}]
    (animation/create (map edn->sprite frames)
                      :frame-duration frame-duration
                      :looping? looping?)))

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defmethods :s/one-to-one
  (malli-form [[_ property-type]]
    (m/qualified-keyword-schema (type->id-namespace property-type)))
  (edn->value [_ property-id]
    (build property-id)))

(defmethods :s/one-to-many
  (malli-form [[_ property-type]]
    (m/set-schema (m/qualified-keyword-schema (type->id-namespace property-type))))
  (edn->value [_ property-ids]
    (set (map build property-ids))))

(defn- map-form [ks]
  (m/map-schema ks (comp malli-form schema-of)))

(defmethod malli-form :s/map [[_ ks]]
  (map-form ks))

(defmethod malli-form :s/map-optional [[_ ks]]
  (map-form (map (fn [k] [k {:optional true}]) ks)))

(defn- namespaced-ks [ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schemas)))

(defmethod malli-form :s/components-ns [[_ ns-name-k]]
  (malli-form [:s/map-optional (namespaced-ks ns-name-k)]))

(defn property->image [{:keys [entity/image entity/animation]}]
  (or image
      (first (:frames animation))))
