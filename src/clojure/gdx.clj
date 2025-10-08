(ns clojure.gdx)

(defprotocol Audio
  (sound [_ path]))

(defprotocol Graphics
  (sprite-batch [_])
  (cursor [_ file-handle [hotspot-x hotspot-y]])
  (truetype-font [_ path params])
  (shape-drawer [_ batch texture-region])
  )
