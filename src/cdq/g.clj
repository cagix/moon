(ns cdq.g)

(defprotocol World
  (spawn-entity! [_ position body components]))
