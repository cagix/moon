(ns cdq.db.schema)

(defprotocol Schema
 :extend-via-metadata true
 (create-value [_ v db]))
