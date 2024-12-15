(ns gdl.malli
  (:require [clojure.set :as set]
            [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg]))

(defn generate [malli-schema-form {:keys [size]}]
  (mg/generate malli-schema-form {:size size}))

(defn- invalid-ex-info [malli-schema value]
  (ex-info (str (me/humanize (m/explain malli-schema value)))
           {:value value
            :schema (m/form malli-schema)}))

(defn validate! [malli-schema-form data]
  (let [malli-schema (m/schema malli-schema-form)]
    (when-not (m/validate malli-schema data)
      (throw (invalid-ex-info malli-schema data)))))

(defn validate [malli-schema data]
  (m/validate malli-schema data))

(defn schema [schema-form]
  (m/schema schema-form))

(def val-max-schema
  (m/schema [:and
             [:vector {:min 2 :max 2} [:int {:min 0}]]
             [:fn {:error/fn (fn [{[^int v ^int mx] :value} _]
                               (when (< mx v)
                                 (format "Expected max (%d) to be smaller than val (%d)" v mx)))}
              (fn [[^int a ^int b]] (<= a b))]]))

(def number-schema  number?)
(def nat-int-schema nat-int?)
(def int-schema     int?)
(def pos-schema     pos?)
(def pos-int-schema pos-int?)
(def string-schema  :string)

(def image-schema
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(def animation-schema
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defn qualified-keyword-schema [namespace]
  [:qualified-keyword {:namespace namespace}])

(defn set-schema [item-schema]
  [:set item-schema])

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

(defn map-keys [m-schema]
  (let [[_m _p & ks] m-schema]
    (for [[k m? _schema] ks]
      k)))

(defn- map-form-k->properties
  "Given a map schema gives a map of key to key properties (like :optional)."
  [m-schema]
  (let [[_m _p & ks] m-schema]
    (into {} (for [[k m? _schema] ks]
               [k (if (map? m?) m?)]))))

(defn optional? [k map-schema]
  (:optional (k (map-form-k->properties map-schema))))

(defn- optional-keyset [m-schema]
  (set (filter #(optional? % m-schema) (map-keys m-schema))))

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

(defn optional-keys-left [m-schema m]
  (seq (set/difference (optional-keyset m-schema)
                       (set (keys m)))))
