(ns cdq.schemas-impl
  (:require [cdq.animation :as animation]
            [cdq.g :as g]
            [cdq.schema :as schema]
            [cdq.schemas :as schemas]
            [cdq.property :as property]
            [cdq.malli :as m]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]
            [gdl.c :as c]
            [gdl.utils]))

(defmulti malli-form (fn [schema _schemas] (schema/type schema)))
(defmethod malli-form :default [schema _schemas] schema)

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

#_(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file ; => this is texture ... convert that key itself only?!
   :sub-image-bounds})

(defn- transform* [schemas property ctx]
  (apply-kvs property
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
    (gdl.utils/safe-get data key))

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

(defn create [data]
  (->Schemas data))

(defmethod malli-form :s/val-max [_ _schemas] val-max/schema)
(defmethod malli-form :s/number  [_ _schemas] number?)
(defmethod malli-form :s/nat-int [_ _schemas] nat-int?)
(defmethod malli-form :s/int     [_ _schemas] int?)
(defmethod malli-form :s/pos     [_ _schemas] pos?)
(defmethod malli-form :s/pos-int [_ _schemas] pos-int?)

(defmethod malli-form :s/sound [_ _schemas] :string)

(defmethod malli-form :s/image [_ _schemas]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod malli-form :s/animation [_ _schemas]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

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

(defn- edn->sprite [{:keys [file sub-image-bounds]} ctx]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (c/sprite-sheet->sprite ctx
                              (c/sprite-sheet ctx file tilew tileh)
                              [(int (/ sprite-x tilew))
                               (int (/ sprite-y tileh))]))
    (c/sprite ctx file)))

(defmethod schema/edn->value :s/image [_ edn ctx]
  (edn->sprite edn ctx))

(defmethod schema/edn->value :s/animation [_ {:keys [frames frame-duration looping?]} ctx]
  (animation/create (map #(edn->sprite % ctx) frames)
                    :frame-duration frame-duration
                    :looping? looping?))

(defmethod schema/edn->value :s/one-to-one [_ property-id ctx]
  (g/build ctx property-id))

(defmethod schema/edn->value :s/one-to-many [_ property-ids ctx]
  (set (map (partial g/build ctx) property-ids)))
