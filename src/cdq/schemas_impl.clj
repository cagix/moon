(ns cdq.schemas-impl
  (:require [cdq.malli :as m]
            [cdq.schema :as schema :refer [malli-form]]
            [cdq.schemas :as schemas]
            [cdq.property :as property]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]))

(defmethod malli-form :s/int     [_ _schemas] int?)
(defmethod malli-form :s/nat-int [_ _schemas] nat-int?)
(defmethod malli-form :s/pos     [_ _schemas] pos?)
(defmethod malli-form :s/pos-int [_ _schemas] pos-int?)
(defmethod malli-form :s/number  [_ _schemas] number?)

(defmethod malli-form :s/one-to-many [[_ property-type] _schemas]
  [:set [:qualified-keyword {:namespace (property/type->id-namespace property-type)}]])

(defmethod malli-form :s/one-to-one [[_ property-type] _schemas]
  [:qualified-keyword {:namespace (property/type->id-namespace property-type)}])

(defmethod malli-form :s/sound [_ _schemas]
  :string)

(defmethod malli-form :s/val-max [_ _schemas]
  val-max/schema)

(defmethod malli-form :s/map [[_ ks] schemas]
  (m/create-map-schema ks (fn [k]
                            (malli-form (get schemas k) schemas))))

(defmethod malli-form :s/map-optional [[_ ks] schemas]
  (malli-form [:s/map (map (fn [k] [k {:optional true}]) ks)]
              schemas))

(defmethod malli-form :s/components-ns [[_ ns-name-k] schemas]
  (malli-form [:s/map-optional (filter #(= (name ns-name-k) (namespace %))
                                       (keys schemas))]
              schemas))

(deftype Schemas [data]
  clojure.lang.ILookup
  (valAt [_ key]
    (utils/safe-get data key))

  schemas/Schemas
  (property-types [_]
    (filter #(= "properties" (namespace %)) (keys data)))

  (validate [_ property]
    (m/form->validate (malli-form (get data (property/type property))
                                  data)
                      property))

  (map-keys [_ map-schema]
    (m/map-keys (malli-form map-schema data)))

  (optional-keyset [_ map-schema]
    (m/optional-keyset (malli-form map-schema data)))

  (optional-k? [_ map-schema k]
    (m/optional? k (malli-form map-schema data)))

  (k->default-value [_ k]
    (let [schema (utils/safe-get data k)]
      (cond
       (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

       ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

       :else (m/generate (malli-form schema data)
                         {:size 3})))))
