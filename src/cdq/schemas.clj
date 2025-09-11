(ns cdq.schemas
  (:require [cdq.malli :as m]
            [cdq.schema :as schema]
            [cdq.property :as property]
            [cdq.utils :as utils]))

(defprotocol Schemas
  (property-types [_])
  (validate [_ property])
  (optional-k? [_ map-schema k])
  (k->default-value [_ k])
  (map-keys [_ map-schema])
  (optional-keyset [_ map-schema]))

(defn build-values [schemas property db]
  (utils/apply-kvs property
                   (fn [k v]
                     (let [schema (get schemas k)
                           v (if (map? v)
                               (build-values schemas v db)
                               v)]
                       (try (schema/create-value schema v db)
                            (catch Throwable t
                              (throw (ex-info " " {:k k :v v} t))))))))


(deftype TSchemas [data]
  clojure.lang.ILookup
  (valAt [_ key]
    (utils/safe-get data key))

  Schemas
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
