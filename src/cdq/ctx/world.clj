(ns cdq.ctx.world)

(defprotocol World
  (active-eids [_])
  (dispose! [_])
  (path-blocked? [_ start target path-w])
  (line-of-sight? [_ source target])
  (find-movement-direction [_ eid]))
