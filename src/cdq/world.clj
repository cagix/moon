(ns cdq.world)

(defprotocol World
  (cache-active-entities [_])
  (update-potential-fields! [_])
  (draw-tiled-map! [_])
  (remove-destroyed-entities! [_])
  (spawn-entity! [_ position body components])
  (position-changed! [_ eid])
  (line-of-sight? [_ source target]))
