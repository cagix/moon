(ns cdq.db)

(defprotocol Database
  (property-types [_])
  (schemas [_])
  (get-raw [_ property-id])
  (build [_ property-id])
  (build-all [_ property-type])
  (update-property [_ property])
  (delete-property [_ property-id]))
