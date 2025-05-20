(ns cdq.schema
  (:refer-clojure :exclude [type]))

(defn type [schema]
  (cond
   (vector? schema) (schema 0)
   (keyword? schema) schema
   :else (throw (IllegalArgumentException. (str "Unkown schema type: " (class schema))))))

(defmulti malli-form (fn [schema _schemas] (type schema)))
(defmethod malli-form :default [schema _schemas] schema)

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

(defmulti edn->value (fn [schema v _ctx]
                       (when schema  ; undefined-data-ks
                         (type schema))))

(defmethod edn->value :default [_schema v _ctx]
  v)

(defn transform [schemas property ctx]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (get schemas k)
                                 (catch Throwable _t
                                   #_(swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (transform schemas v ctx)
                         v)]
                 (try (edn->value schema v ctx)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))
