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

(defn default-value [schemas k]
  (let [schema (utils/safe-get schemas k)]
    (cond
     ;(#{:s/one-to-one :s/one-to-many} (get-type schema)) nil
     ;(#{:s/map} type) {} ; cannot have empty for required keys, then no Add Component button
     :else (m/generate (schema/malli-form schema schemas)
                       {:size 3}))))

(defn validate [schemas k value]
  (-> (get schemas k)
      (schema/malli-form schemas)
      m/schema
      (m/validate-humanize value)))

(defn create-map-schema [schemas ks]
  (m/create-map-schema ks (fn [k]
                            (schema/malli-form (get schemas k) schemas))) )
