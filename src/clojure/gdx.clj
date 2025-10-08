(ns clojure.gdx)

(defprotocol Audio
  (sound [_ path]))

(defprotocol Files
  (search-files [_ {:keys [folder extensions]}]))

(defprotocol Graphics
  (sprite-batch [_])
  (cursor [_ file-handle [hotspot-x hotspot-y]])
  (truetype-font [_ path params])
  (shape-drawer [_ batch texture-region])
  (texture [_ path])
  (viewport [_ world-width world-height camera]))
