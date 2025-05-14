(ns cdq.impl.schema
  (:require [cdq.schema :as schema]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]))

(defmethod schema/malli-form :s/val-max [_ _schemas] val-max/schema)
(defmethod schema/malli-form :s/number  [_ _schemas] number?)
(defmethod schema/malli-form :s/nat-int [_ _schemas] nat-int?)
(defmethod schema/malli-form :s/int     [_ _schemas] int?)
(defmethod schema/malli-form :s/pos     [_ _schemas] pos?)
(defmethod schema/malli-form :s/pos-int [_ _schemas] pos-int?)

(defmethod schema/malli-form :s/sound [_ _schemas] :string)

(defmethod schema/malli-form :s/image [_ _schemas]
  [:map {:closed true}
   [:file :string]
   [:sub-image-bounds {:optional true} [:vector {:size 4} nat-int?]]])

(defmethod schema/malli-form :s/animation [_ _schemas]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defn- type->id-namespace [property-type]
  (keyword (name property-type)))

(defmethod schema/malli-form :s/one-to-one [[_ property-type] _schemas]
  [:qualified-keyword {:namespace (type->id-namespace property-type)}])

(defmethod schema/malli-form :s/one-to-many [[_ property-type] _schemas]
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
                   (schema/malli-form (utils/safe-get schemas k)
                                      schemas))))

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
