(ns cdq.raycaster)

(defprotocol Raycaster
  (blocked? [_ start end])
  (path-blocked? [_ start target path-w])
  (line-of-sight? [_ source target]))
