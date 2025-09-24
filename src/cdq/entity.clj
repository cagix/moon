(ns cdq.entity)

(defprotocol Entity
  (position [_])
  (distance [_ other-entity]))
