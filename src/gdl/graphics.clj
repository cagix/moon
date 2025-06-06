(ns gdl.graphics)

(defprotocol Graphics
  (delta-time [_])
  (frames-per-second [_])
  (set-cursor! [_ cursor]))
