(ns cdq.schemas-impl
  (:require [cdq.malli :as m]
            [cdq.schema :as schema]
            [cdq.schemas :as schemas]
            [cdq.property :as property]
            [cdq.utils :as utils]))

(deftype Schemas [data]
  clojure.lang.ILookup
  (valAt [_ key]
    (utils/safe-get data key))

  schemas/Schemas
  (property-types [_]
    (filter #(= "properties" (namespace %)) (keys data)))

  (validate [_ property]
    (m/form->validate (schema/malli-form (get data (property/type property))
                                  data)
                      property))

  (map-keys [_ map-schema]
    (m/map-keys (schema/malli-form map-schema data)))

  (optional-keyset [_ map-schema]
    (m/optional-keyset (schema/malli-form map-schema data)))

  (optional-k? [_ map-schema k]
    (m/optional? k (schema/malli-form map-schema data)))

  (k->default-value [_ k]
    (let [schema (utils/safe-get data k)]
      (cond
       (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

       ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

       :else (m/generate (schema/malli-form schema data)
                         {:size 3})))))
