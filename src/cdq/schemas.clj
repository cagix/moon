(ns cdq.schemas)

(defprotocol Schemas
  (schema [_ k])
  (optional-k? [_ schema k])
  (generate [_ schema {:keys [size]}])
  (map-keys [_ schema])
  (optional-keyset [_ schema])
  (property-types [_])
  (validate! [_ property-type property])
  (transform [_ property]))
