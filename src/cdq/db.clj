(ns cdq.db)

(defprotocol Database
  (property-types [_])
  (update! [_ property])
  (delete! [_ property-id])
  (get-raw [_ property-id])
  (all-raw [_ property-type])
  (build [_ property-id])
  (build-all [_ property-type]))
