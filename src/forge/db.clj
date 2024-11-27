(ns forge.db
  (:refer-clojure :exclude [get])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [forge.graphics :as g]
            [forge.graphics.animation :as animation]
            [forge.utils :refer [safe-get]]
            [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg]))

(declare ^:private schemas
         ^:private properties-file
         ^:private db)

(defn schema-of [k]
  {:pre [(contains? schemas k)]}
  (clojure.core/get schemas k))

(defn schema-type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defmulti malli-form schema-type)
(defmethod malli-form :default [schema] schema)

(defmethod malli-form :s/number  [_] number?)
(defmethod malli-form :s/nat-int [_] nat-int?)
(defmethod malli-form :s/int     [_] int?)
(defmethod malli-form :s/pos     [_] pos?)
(defmethod malli-form :s/pos-int [_] pos-int?)

(defmethod malli-form :s/sound [_] :string)

(defmethod malli-form :s/image [_]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod malli-form :s/animation [_]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defmethod malli-form :s/one-to-one [[_ property-type]]
  [:qualified-keyword {:namespace (type->id-namespace property-type)}])

(defmethod malli-form :s/one-to-many [[_ property-type]]
  [:set [:qualified-keyword {:namespace (type->id-namespace property-type)}]])

(defn- attribute-form
  "Can define keys as just keywords or with schema-props like [:foo {:optional true}]."
  [ks]
  (for [k ks
        :let [k? (keyword? k)
              schema-props (if k? nil (k 1))
              k (if k? k (k 0))]]
    (do
     (assert (keyword? k))
     (assert (or (nil? schema-props) (map? schema-props)) (pr-str ks))
     [k schema-props (malli-form (schema-of k))])))

(defn- map-form [ks]
  (apply vector :map {:closed true} (attribute-form ks)))

(defmethod malli-form :s/map [[_ ks]]
  (map-form ks))

(defmethod malli-form :s/map-optional [[_ ks]]
  (map-form (map (fn [k] [k {:optional true}]) ks)))

(defn- namespaced-ks [ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schemas)))

(defmethod malli-form :s/components-ns [[_ ns-name-k]]
  (malli-form [:s/map-optional (namespaced-ks ns-name-k)]))

(defn property-type [{:keys [property/id]}]
  (keyword "properties" (namespace id)))

(defn property-types []
  (filter #(= "properties" (namespace %))
          (keys schemas)))

(defn schema-of-property [property]
  (schema-of (property-type property)))

(defn- invalid-ex-info [m-schema value]
  (ex-info (str (me/humanize (m/explain m-schema value)))
           {:value value
            :schema (m/form m-schema)}))

(defn- validate! [property]
  (let [m-schema (-> property
                     schema-of-property
                     malli-form
                     m/schema)]
    (when-not (m/validate m-schema property)
      (throw (invalid-ex-info m-schema property)))))

(defn k->default-value [k]
  (let [schema (schema-of k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema-type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (mg/generate (malli-form schema) {:size 3}))))

(defn init [& {:keys [schema properties]}]
  (bind-root #'schemas (-> schema io/resource slurp edn/read-string))
  (bind-root #'properties-file (io/resource properties))
  (let [properties (-> properties-file slurp edn/read-string)]
    (assert (or (empty? properties)
                (apply distinct? (map :property/id properties))))
    (run! validate! properties)
    (bind-root #'db (zipmap (map :property/id properties) properties))))

(defn- async-pprint-spit! [properties]
  (.start
   (Thread.
    (fn []
      (binding [*print-level* nil]
        (->> properties
             pprint
             with-out-str
             (spit properties-file)))))))

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
       (sort-by property-type)
       (map recur-sort-map)
       doall
       async-pprint-spit!))

(defn get-raw [id]
  (safe-get db id))

(defn all-raw [type]
  (->> (vals db)
       (filter #(= type (property-type %)))))

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

(declare get)

(defmulti edn->value (fn [schema v]
                       (when schema  ; undefined-data-ks
                         (schema-type schema))))
(defmethod edn->value :default [_schema v] v)

(defmethod edn->value :s/one-to-many [_ property-ids]
  (set (map get property-ids)))

(defmethod edn->value :s/one-to-one [_ property-id]
  (get property-id))

(defn- edn->image [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (g/sprite (g/sprite-sheet file tilew tileh)
                [(int (/ sprite-x tilew))
                 (int (/ sprite-y tileh))]))
    (g/image file)))

(defmethod edn->value :s/image [_ edn]
  (edn->image edn))

(defmethod edn->value :s/animation [_ {:keys [frames frame-duration looping?]}]
  (animation/create (map edn->image frames)
                    :frame-duration frame-duration
                    :looping? looping?))

(defn- build [property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema-of k)
                                 (catch Throwable _t
                                   (swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build v)
                         v)]
                 (try (edn->value schema v)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

(defn get [id]
  (build (get-raw id)))

(defn all [type]
  (map build (all-raw type)))

(defn update! [{:keys [property/id] :as property}]
  {:pre [(contains? property :property/id)
         (contains? db id)]}
  (validate! property)
  (alter-var-root #'db assoc id property)
  (async-write-to-file!))

(defn delete! [property-id]
  {:pre [(contains? db property-id)]}
  (alter-var-root #'db dissoc property-id)
  (async-write-to-file!))

(defn migrate [property-type update-fn]
  (doseq [id (map :property/id (all-raw property-type))]
    (println id)
    (alter-var-root #'db update id update-fn))
  (async-write-to-file!))
