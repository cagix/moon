(ns cdq.db.schema.some)

(defn malli-form [[_ & params] _schemas]
  :some)

(defn create-value [_ v _db]
  v)
