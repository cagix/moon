(ns forge.schema
  (:require [malli.core :as m]
            [malli.error :as me]
            [malli.generator :as mg])
  (:refer-clojure :exclude [type]))

(declare ^:private schemas)

(defn init [data]
  (.bindRoot #'schemas data))

(defn of [k]
  {:pre [(contains? schemas k)]}
  (get schemas k))

(defn type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defmulti form type)
(defmethod form :default [schema] schema)

(defmethod form :s/number  [_] number?)
(defmethod form :s/nat-int [_] nat-int?)
(defmethod form :s/int     [_] int?)
(defmethod form :s/pos     [_] pos?)
(defmethod form :s/pos-int [_] pos-int?)

(defmethod form :s/sound [_] :string)

(defmethod form :s/image [_]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod form :s/animation [_]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defmethod form :s/one-to-one [[_ property-type]]
  [:qualified-keyword {:namespace (type->id-namespace property-type)}])

(defmethod form :s/one-to-many [[_ property-type]]
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
     [k schema-props (form (of k))])))

(defn- map-form [ks]
  (apply vector :map {:closed true} (attribute-form ks)))

(defmethod form :s/map [[_ ks]]
  (map-form ks))

(defmethod form :s/map-optional [[_ ks]]
  (map-form (map (fn [k] [k {:optional true}]) ks)))

(defn- namespaced-ks [ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys schemas)))

(defmethod form :s/components-ns [[_ ns-name-k]]
  (form [:s/map-optional (namespaced-ks ns-name-k)]))

(defn property-type [{:keys [property/id]}]
  (keyword "properties" (namespace id)))

(defn property-types []
  (filter #(= "properties" (namespace %))
          (keys schemas)))

(defn of-property [property]
  (of (property-type property)))

(defn- invalid-ex-info [m-schema value]
  (ex-info (str (me/humanize (m/explain m-schema value)))
           {:value value
            :schema (m/form m-schema)}))

(defn validate! [property]
  (let [m-schema (-> property
                     of-property
                     form
                     m/schema)]
    (when-not (m/validate m-schema property)
      (throw (invalid-ex-info m-schema property)))))

(defn k->default-value [k]
  (let [schema (of k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (mg/generate (form schema) {:size 3}))))
