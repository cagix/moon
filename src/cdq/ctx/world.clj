(ns cdq.ctx.world)

(defprotocol Resettable
  (reset-state [_ world-fn-result]))

(defprotocol World
  (dispose! [_])
  (active-eids [_]))

(defprotocol MovementAI
  (find-movement-direction [_ eid]))

(defprotocol RayCaster
  (ray-blocked? [_ start target])
  (path-blocked? [_ start target path-w])
  (line-of-sight? [_ source target]))
