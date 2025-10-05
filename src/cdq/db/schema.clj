(ns cdq.db.schema)

(defprotocol Schema
 :extend-via-metadata true
 (malli-form [_ schemas])
 (create-value [_ v db]))
