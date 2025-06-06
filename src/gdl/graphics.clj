(ns gdl.graphics)

(defprotocol Graphics
  (delta-time [_])
  (frames-per-second [_])
  (set-cursor! [_ cursor-key])
  (texture [_ path])
  (draw-on-world-viewport! [_ f])
  (draw-tiled-map! [_ tiled-map color-setter])
  )
