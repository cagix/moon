(ns cdq.schemas)

(defprotocol Schemas
  (property-types [_])
  (validate! [_ property-type property])
  (transform [_ property]))
