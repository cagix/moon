(ns cdq.graphics)

(defprotocol Graphics
  (clear! [_ [r g b a]])
  (set-cursor! [_ cursor-key])
  (delta-time [_])
  (frames-per-second [_]))
