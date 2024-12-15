; Release as separate library with javafx app
; and way to add schemas inside the app (build up your database....)
; and decouple schema from malli etc.
; even how the property-types are handled.....?

; wait why do I even need property-types ?
; we just validate each property against its key
; as we know we are dealing with a thing when we use it ?

; even the dev-loop itself....


; Idea: property/type remove, make whole 'db-schema' instead of schemas
; db is map of {:audiovisuals, creatures, etc.}
; and we know when we fetch something against what to check it ?
; also complete db schema also textures -> image :file is getting to texture
; file is one-to-one texture ? or spritesheets we can have as separate properties ?!

; But first make context-free db so I can abstract mallis/schema, .... its just API -> optional-map-keys/validate something ...
(ns gdl.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.graphics.animation :as animation]
            [gdl.graphics.sprite :as sprite]
            [gdl.schema :as schema]
            [gdl.utils :refer [safe-get recur-sort-map apply-kvs async-pprint-spit! defmethods]]
            [gdl.val-max :as val-max]
            [malli.core :as m]
            [malli.error :as me]))

(defn schema-type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defn- property-type [{:keys [property/id]}]
  (keyword "properties" (namespace id)))

(declare schemas)

(defn property-types []
  (filter #(= "properties" (namespace %))
          (keys schemas)))

(defn schema-of [k]
  (safe-get schemas k))

(defn schema-of-property [property]
  (schema-of (property-type property)))

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


(defn- invalid-ex-info [m-schema value]
  (ex-info (str (me/humanize (m/explain m-schema value)))
           {:value value
            :schema (m/form m-schema)}))

(defn- property->m-schema [property]
  (try (-> property
           schema-of-property
           malli-form
           m/schema)
       (catch clojure.lang.ExceptionInfo e
         (throw (ex-info "property->m-schema fail"
                         (merge (ex-data e)
                                {:property/id (:property/id property)
                                 :property property
                                 :schema-of-property (schema-of-property property)
                                 :malli-form (malli-form (schema-of-property property))}))))))

(defn validate! [property]
  (let [m-schema (property->m-schema property)]
    (when-not (m/validate m-schema property)
      (throw (invalid-ex-info m-schema property)))))

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

(defmethod malli-form :s/val-max [_] (m/form val-max/schema))

(defmethod malli-form :s/number  [_] number?)
(defmethod malli-form :s/nat-int [_] nat-int?)
(defmethod malli-form :s/int     [_] int?)
(defmethod malli-form :s/pos     [_] pos?)
(defmethod malli-form :s/pos-int [_] pos-int?)

(defmethod malli-form :s/sound [_] :string)

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
    [:map {:closed true}
     [:file :string]
     [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

  (edn->value [_ edn]
    (edn->sprite edn)))

(defmethods :s/animation
  (malli-form [_]
    [:map {:closed true}
     [:frames :some] ; FIXME actually images
     [:frame-duration pos?]
     [:looping? :boolean]])

  (edn->value [_ {:keys [frames frame-duration looping?]}]
    (animation/create (map edn->sprite frames)
                      :frame-duration frame-duration
                      :looping? looping?)))

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defmethods :s/one-to-one
  (malli-form [[_ property-type]]
    [:qualified-keyword {:namespace (type->id-namespace property-type)}])
  (edn->value [_ property-id]
    (build property-id)))

(defmethods :s/one-to-many
  (malli-form [[_ property-type]]
    [:set [:qualified-keyword {:namespace (type->id-namespace property-type)}]])
  (edn->value [_ property-ids]
    (set (map build property-ids))))

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

(defn property->image [{:keys [entity/image entity/animation]}]
  (or image
      (first (:frames animation))))
