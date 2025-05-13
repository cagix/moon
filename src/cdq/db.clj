(ns cdq.db
  (:refer-clojure :exclude [update]))

(defprotocol DB
  (update [_ property])
  (delete [_ property-id])
  (save! [_])
  (get-raw [_ property-id])
  (all-raw [_ property-type])
  (build [_ property-id])
  (build-all [_ property-type]))
