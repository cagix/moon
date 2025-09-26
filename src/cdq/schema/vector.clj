(ns cdq.schema.vector)

(defn malli-form [[_ & params] _schemas]
  (apply vector :vector params))

(defn create-value [_ v _db]
  v)
