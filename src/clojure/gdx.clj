(ns clojure.gdx)

(defprotocol Files
  (search-files [_ {:keys [folder extensions]}]))

(defprotocol Graphics
  (sprite-batch [_])
  (cursor [_ file-handle [hotspot-x hotspot-y]])
  (truetype-font [_ path params])
  (shape-drawer [_ batch texture-region])
  (texture [_ path])
  (viewport [_ world-width world-height camera])
  (delta-time [_])
  (frames-per-second [_])
  (set-cursor! [_ cursor])
  (clear! [_ [r g b a]])
  (orthographic-camera [_]
                       [_ {:keys [y-down? world-width world-height]}]))
