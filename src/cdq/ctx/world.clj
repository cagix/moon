(ns cdq.ctx.world)

(defprotocol Resettable
  (reset-state [_ world-fn-result]))

(defprotocol World
  (dispose! [_])
  (active-eids [_])
  (path-blocked? [_ start target path-w])
  (line-of-sight? [_ source target])
  (find-movement-direction [_ eid]))
