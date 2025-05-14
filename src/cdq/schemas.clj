(ns cdq.schemas)

(defprotocol Schemas
  (schema [_ k])
  (property-types [_])
  (validate! [_ property-type property])
  (transform [_ property]))
