(ns clojure.gdx.graphics)

(defprotocol Graphics
  (delta-time [_])
  (frames-per-second [_])
  (cursor [_ pixmap hotspot-x hotspot-y])
  (set-cursor! [_ cursor])
  (pixmap [_ file-handle]
          [_ width height format])
  (texture [_ pixmap]))
