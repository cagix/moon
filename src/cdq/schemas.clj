(ns cdq.schemas
  (:require [cdq.malli :as m]
            [cdq.schema :as schema]))

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

(defn build-values [schemas property db]
  (apply-kvs property
             (fn [k v]
               (try (schema/create-value (get schemas k) v db)
                    (catch Throwable t
                      (throw (ex-info " " {:k k :v v} t)))))))

(defn default-value [schemas k]
  (let [schema (get schemas k)]
    (cond
     ;(#{:s/one-to-one :s/one-to-many} (get-type schema)) nil
     (#{:s/map} (schema 0)) {} ; cannot have empty for required keys, then no Add Component button
     :else nil
     ;:else (m/generate (schema/malli-form schema schemas) {:size 3})

     )))

(defn validate [schemas k value]
  (-> (get schemas k)
      (schema/malli-form schemas)
      m/schema
      (m/validate-humanize value)))

(defn create-map-schema [schemas ks]
  (m/create-map-schema ks (fn [k]
                            (schema/malli-form (get schemas k) schemas))) )
