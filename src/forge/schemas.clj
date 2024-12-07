(ns forge.schemas
  (:require [forge.animation :as animation]
            [forge.app.db :as db :refer [malli-form
                                         edn->value]]
            [forge.graphics :refer [edn->image]]
            [forge.val-max :as val-max]
            [malli.core :as m]))

(defmethod malli-form :s/val-max [_] (m/form val-max/schema))

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
     [k schema-props (db/malli-form (db/schema-of k))])))

(defn- map-form [ks]
  (apply vector :map {:closed true} (attribute-form ks)))

(defmethod malli-form :s/map [[_ ks]]
  (map-form ks))

(defmethod malli-form :s/map-optional [[_ ks]]
  (map-form (map (fn [k] [k {:optional true}]) ks)))

(defn- namespaced-ks [ns-name-k]
  (filter #(= (name ns-name-k) (namespace %))
          (keys db/schemas)))

(defmethod malli-form :s/components-ns [[_ ns-name-k]]
  (malli-form [:s/map-optional (namespaced-ks ns-name-k)]))

(defmethod edn->value :s/one-to-many [_ property-ids]
  (set (map db/build property-ids)))

(defmethod edn->value :s/one-to-one [_ property-id]
  (db/build property-id))

(defmethod edn->value :s/image [_ edn]
  (edn->image edn))

(defmethod edn->value :s/animation [_ {:keys [frames frame-duration looping?]}]
  (animation/create (map edn->image frames)
                    :frame-duration frame-duration
                    :looping? looping?))
