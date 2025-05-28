(ns gdl.graphics)

(defprotocol Graphics
  (new-cursor [_ pixmap hotspot-x hotspot-y])
  (delta-time [_])
  (set-cursor! [_ cursor])
  (frames-per-second [_])
  (clear-screen! [_]))
