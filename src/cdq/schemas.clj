(ns cdq.schemas
  (:require [cdq.malli :as m]
            [cdq.schema :as schema]
            [cdq.utils :as utils]))

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

(defn validate [schemas k value]
  (m/form->validate (schema/malli-form (get schemas k) schemas)
                    value))

(defn map-keys [schemas map-schema]
  (m/map-keys (schema/malli-form map-schema schemas)))

(defn optional-keyset [schemas map-schema]
  (m/optional-keyset (schema/malli-form map-schema schemas)))

(defn optional-k? [schemas map-schema k]
  (m/optional? k (schema/malli-form map-schema schemas)))

(defn k->default-value [schemas k]
  (let [schema (utils/safe-get schemas k)]
    (cond
     (#{:s/one-to-one :s/one-to-many} (schema/type schema)) nil

     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button

     :else (m/generate (schema/malli-form schema schemas)
                       {:size 3}))))
