(ns cdq.schemas
  (:require [cdq.schema :as schema]
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
