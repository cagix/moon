(ns cdq.world)

(defprotocol World
  (cache-active-entities [_])
  (spawn-entity! [_ position body components])
  (remove-entity! [_ eid])
  (position-changed! [_ eid])
  (cell [_ position])
  (line-of-sight? [_ source target]))
