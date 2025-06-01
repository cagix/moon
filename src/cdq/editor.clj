(ns cdq.editor)

(defprotocol Editor
  (open-editor-window! [_ property-type])
  (edit-property! [_ property])
  (property-overview-table [_ property-type clicked-id-fn]))
