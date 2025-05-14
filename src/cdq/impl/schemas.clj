(ns cdq.impl.schemas
  (:require [cdq.schema :as schema]
            [cdq.schemas :as schemas]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]
            [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg]))

(defn- map-keys [map-schema]
  (let [[_m _p & ks] map-schema]
    (for [[k m? _schema] ks]
      k)))

(comment
 (= (map-keys
     [:map {:closed true}
      [:foo]
      [:bar]
      [:baz {:optional true}]
      [:boz {:optional false}]
      [:asdf {:optional true}]])
    [:foo :bar :baz :boz :asdf]))

(defn- map-form-k->properties
  "Given a map schema gives a map of key to key properties (like :optional)."
  [map-schema]
  (let [[_m _p & ks] map-schema]
    (into {} (for [[k m? _schema] ks]
               [k (if (map? m?) m?)]))))

(comment
 (= (map-form-k->properties
     [:map {:closed true}
      [:foo]
      [:bar]
      [:baz {:optional true}]
      [:boz {:optional false}]
      [:asdf {:optional true}]])
    {:foo nil,
     :bar nil,
     :baz {:optional true},
     :boz {:optional false},
     :asdf {:optional true}}))

(defn- optional? [k map-schema]
  (:optional (k (map-form-k->properties map-schema))))

(comment
 (= (optional? :foo
     [:map {:closed true}
      [:foo]
      [:bar]
      [:baz {:optional true}]
      [:boz {:optional false}]
      [:asdf {:optional true}]])
    nil)

 (= (optional? :baz
               [:map {:closed true}
                [:foo]
                [:bar]
                [:baz {:optional true}]
                [:boz {:optional false}]
                [:asdf {:optional true}]])
    true)

 (= (optional? :asdf
               [:map {:closed true}
                [:foo]
                [:bar]
                [:baz {:optional true}]
                [:boz {:optional false}]
                [:asdf {:optional true}]])
    true)
 )

(defn- optional-keyset [map-schema]
  (set (filter #(optional? % map-schema)
               (map-keys map-schema))))

(comment
 (= (optional-keyset
     [:map {:closed true}
      [:foo]
      [:bar]
      [:baz {:optional true}]
      [:boz {:optional false}]
      [:asdf {:optional true}]])
    #{:baz :asdf})
 )

(defn- validate! [form value]
  (let [schema (m/schema form)]
    (when-not (m/validate schema value)
      (throw (ex-info (str (me/humanize (m/explain schema value)))
                      {:value value
                       :schema (m/form schema)})))))

(comment
 (validate! [:map {:closed true}
             [:foo pos?]
             [:bar pos?]
             [:baz {:optional true} :some]
             [:boz {:optional false} :some]
             [:asdf {:optional true} :some]]
            {:foo 1
             :bar 2
             :boz :a
             :asdf :b
             :baz :asdf})

 (validate! [:map {:closed true}
             [:foo pos?]
             [:bar pos?]
             [:baz {:optional true} :some]
             [:boz {:optional false} :some]
             [:asdf {:optional true} :some]]
            {:foo 1
             :bar 2
             :boz :a})

 (validate! [:map {:closed true}
             [:foo pos?]
             [:bar pos?]
             [:baz {:optional true} :some]
             [:boz {:optional false} :some]
             [:asdf {:optional true} :some]]
            {:bar 2
             :boz :a})
 )

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

(defmulti malli-form (fn [schema _schemas] (schema/type schema)))
(defmethod malli-form :default [schema _schemas] schema)

(defrecord Schemas [schemas]
  schemas/Schemas
  (schema [_ k]
    (utils/safe-get schemas k))

  (optional-k? [_ schema k]
    (optional? k (malli-form schema schemas)))

  (generate [_ schema {:keys [size]}]
    (mg/generate (malli-form schema schemas)
                 {:size size}))

  (map-keys [_ schema]
    (map-keys (malli-form schema schemas)))

  (optional-keyset [_ schema]
    (optional-keyset (malli-form schema schemas)))

  (property-types [_]
    (filter #(= "properties" (namespace %)) (keys schemas)))

  (validate! [this k value]
    (validate! (malli-form (schemas/schema this k) schemas)
               value))

  (transform [this property]
    (apply-kvs property
               (fn [k v]
                 (let [schema (try (schemas/schema this k)
                                   (catch Throwable _t
                                     #_(swap! undefined-data-ks conj k)
                                     nil))
                       v (if (map? v)
                           (schemas/transform this v)
                           v)]
                   (try (schema/edn->value schema v)
                        (catch Throwable t
                          (throw (ex-info " " {:k k :v v} t)))))))))

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
                   (malli-form (utils/safe-get schemas k)
                               schemas))))

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
