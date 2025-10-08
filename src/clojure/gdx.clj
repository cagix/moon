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
  (viewport [_ world-width world-height camera])
  (delta-time [_])
  (frames-per-second [_])
  (set-cursor! [_ cursor])
  (clear! [_ [r g b a]]))

(defprotocol Input
  (set-input-processor! [_ input-processor])
  (key-pressed? [_ key])
  (key-just-pressed? [_ key])
  (button-just-pressed? [_ button])
  (mouse-position [_])
  )
