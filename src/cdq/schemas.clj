(ns cdq.schemas)

(defprotocol Schemas
  (validate! [_ property-type property])
  (transform [_ property]))
