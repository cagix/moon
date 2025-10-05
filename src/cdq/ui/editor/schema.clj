(ns cdq.ui.editor.schema)

(defprotocol Schema
 :extend-via-metadata true
 (create [_ v ctx])
 (value [_ widget schemas]))
