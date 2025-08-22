(ns cdq.schemas)

(defprotocol Schemas
  (property-types [_])
  (validate [_ property])
  (optional-k? [_ map-schema k])
  (k->default-value [_ k])
  (map-keys [_ map-schema])
  (optional-keyset [_ map-schema]))
