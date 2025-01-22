(ns cdq.db
  (:refer-clojure :exclude [update]))

(defprotocol DB
  (update [_ property schemas])
  (delete [_ property-id])
  (async-write-to-file! [_])
  (get-raw [_ property-id])
  (all-raw [_ property-type])
  (build     [_ property-id   context])
  (build-all [_ property-type context]))
