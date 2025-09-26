(ns cdq.db.schema.number)

(defn malli-form [[_ predicate] _schemas]
  (case predicate
    :int     int?
    :nat-int nat-int?
    :any     number?
    :pos     pos?
    :pos-int pos-int?))

(defn create-value [_ v _db]
  v)
