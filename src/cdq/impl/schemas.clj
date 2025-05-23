(ns cdq.impl.schemas
  (:require [cdq.animation :as animation]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.schema :as schema]
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
                   (schema/malli-form (get schemas k) schemas))))

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

(defn- edn->sprite
  [{:keys [file sub-image-bounds]}
   {:keys [ctx/assets
           ctx/world-unit-scale]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (graphics/from-sheet (graphics/sprite-sheet (assets file)
                                                  tilew
                                                  tileh
                                                  world-unit-scale)
                           [(int (/ sprite-x tilew))
                            (int (/ sprite-y tileh))]
                           world-unit-scale))
    (graphics/sprite (assets file)
                     world-unit-scale)))

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
