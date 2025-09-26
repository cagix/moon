(ns cdq.schema.qualified-keyword)

(defn malli-form [[_ & params] _schemas]
  (apply vector :qualified-keyword params))

(defn create-value [_ v _db]
  v)
