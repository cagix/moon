(ns cdq.impl.schemas
  (:require [cdq.db.schema :as schema]
            [cdq.schemas :as schemas]))

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

#_(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file ; => this is texture ... convert that key itself only?!
   :sub-image-bounds})

(defrecord Schemas [schemas]
  schemas/Schemas
  (property-types [_]
    (filter #(= "properties" (namespace %)) (keys schemas)))

  (validate! [_ property-type property]
    (schema/validate! schemas property-type property))

  (transform [this property]
    (apply-kvs property
               (fn [k v]
                 (let [schema (try (schema/schema-of schemas k)
                                   (catch Throwable _t
                                     #_(swap! undefined-data-ks conj k)
                                     nil))
                       v (if (map? v)
                           (schemas/transform this v)
                           v)]
                   (try (schema/edn->value schema v)
                        (catch Throwable t
                          (throw (ex-info " " {:k k :v v} t)))))))))

(defn create [data]
  (->Schemas data))
