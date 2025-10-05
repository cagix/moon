(ns cdq.graphics)

(defprotocol PGraphics
  (clear! [_ [r g b a]])
  (draw-tiled-map! [_ tiled-map color-setter])
  (set-cursor! [_ cursor-key])
  (delta-time [_])
  (frames-per-second [_])
  (update-viewports! [_ width height])
  (unproject-ui [_ position]))
