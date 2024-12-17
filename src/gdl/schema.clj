(ns gdl.schema
  (:refer-clojure :exclude [type])
  (:require [gdl.property :as property]
            [gdl.malli :as m]
            [gdl.utils :refer [safe-get apply-kvs]]))

(defn type [schema]
  (if (vector? schema)
    (schema 0)
    schema))

(defmulti malli-form (fn [schema _schemas] (type schema)))
(defmethod malli-form :default [schema _schemas] schema)

(defmulti edn->value (fn [schema v]
                       (when schema  ; undefined-data-ks
                         (type schema))))
(defmethod edn->value :default [_schema v] v)

(defn property-types [schemas]
  (filter #(= "properties" (namespace %))
          (keys schemas)))

; TODO ... malli is internal to 'schema' ns ?!
; (m/generate (db/malli-form schema) {:size 3})
(defn schema-of [schemas k]
  (assert (contains? schemas k)
          (pr-str k))
  (safe-get schemas k))

(defn validate! [schemas property]
  (m/validate! (malli-form (schema-of schemas (property/type property))
                           schemas)
               property))

#_(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file ; => this is texture ... convert that key itself only?!
   :sub-image-bounds})

(defn build [schemas property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema-of schemas k)
                                 (catch Throwable _t
                                   #_(swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build schemas v)
                         v)]
                 (try (edn->value schema v)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))
