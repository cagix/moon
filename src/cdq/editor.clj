(ns cdq.editor)

(defprotocol Editor
  (overview-table-rows [ctx property-type clicked-id-fn]))
