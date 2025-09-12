(ns cdq.schema)

(defn- get-type [schema]
  (assert (vector? schema))
  (schema 0))

(declare k->methods)

(defn malli-form [schema schemas]
  ((:malli-form (k->methods (get-type schema))) schema schemas))

(defn create-value [schema v db]
  (if-let [f (:create-value (k->methods (get-type schema)))]
    (f schema v db)
    v))

(defn create [schema v ctx]
  ((:create (k->methods (get-type schema))) schema v ctx))

(defn value [schema widget schemas]
  ((:value (k->methods (get-type schema))) schema widget schemas))
