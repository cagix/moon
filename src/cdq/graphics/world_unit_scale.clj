(ns cdq.graphics.world-unit-scale)

(defn create [context config]
  (assoc context :gdl.graphics/world-unit-scale (float (/ (::tile-size config)))))
