(ns cdq.db.schema
  (:refer-clojure :exclude [type])
  (:require [clojure.set :as set]
            [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg]))

(defn type [schema]
  (cond
   (vector? schema) (schema 0)
   (keyword? schema) schema
   :else (throw (IllegalArgumentException. (str "Unkown schema type: " (class schema))))))

(defmulti malli-form (fn [schema _schemas] (type schema)))
(defmethod malli-form :default [schema _schemas] schema)

(defn schema-of [schemas k]
  (assert (contains? schemas k) (pr-str k))
  (get schemas k))

(defn- invalid-ex-info [malli-schema value]
  (ex-info (str (me/humanize (m/explain malli-schema value)))
           {:value value
            :schema (m/form malli-schema)}))

(defn validate [malli-schema value]
  (m/validate malli-schema value))

(defn m-schema [malli-form]
  (m/schema malli-form))

(defn validate! [schemas schema-k value]
  (let [malli-schema (m-schema (malli-form (schema-of schemas schema-k)
                                           schemas))]
    (when-not (validate malli-schema value)
      (throw (invalid-ex-info malli-schema value)))))

(defn generate [schema size schemas]
  (mg/generate (malli-form schema schemas)
               {:size 3}))

(defn map-keys [schema schemas]
  (let [[_m _p & ks] (malli-form schema schemas)]
    (for [[k m? _schema] ks]
      k)))

(defn- map-form-k->properties
  "Given a map schema gives a map of key to key properties (like :optional)."
  [m-schema]
  (let [[_m _p & ks] m-schema]
    (into {} (for [[k m? _schema] ks]
               [k (if (map? m?) m?)]))))

(defn- optional? [k map-schema]
  (:optional (k (map-form-k->properties map-schema))))

(defn optional-k? [k schema schemas]
  (optional? k (malli-form schema schemas)))

(defn- optional-keyset [m-schema schemas]
  (set (filter #(optional? % m-schema) (map-keys m-schema
                                                 schemas))))

(comment
 (= (optional-keyset
     [:map {:closed true}
      [:foo]
      [:bar]
      [:baz {:optional true}]
      [:boz {:optional false}]
      [:asdf {:optional true}]])
    [:baz :asdf])
 )

(defn optional-keys-left [schema m schemas]
  (seq (set/difference (optional-keyset (malli-form schema schemas)
                                        schemas)
                       (set (keys m)))))

(def val-max-schema
  (m/schema [:and
             [:vector {:min 2 :max 2} [:int {:min 0}]]
             [:fn {:error/fn (fn [{[^int v ^int mx] :value} _]
                               (when (< mx v)
                                 (format "Expected max (%d) to be smaller than val (%d)" v mx)))}
              (fn [[^int a ^int b]] (<= a b))]]))

(defmethod malli-form :s/val-max [_ _schemas] val-max-schema)
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

(defn map-schema
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
                   (malli-form (schema-of schemas k)
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
