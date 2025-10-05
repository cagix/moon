(ns cdq.graphics)

(defprotocol PGraphics
  (clear! [_ [r g b a]])
  (draw-tiled-map! [_ tiled-map color-setter])
  (set-cursor! [_ cursor-key])
  (delta-time [_])
  (frames-per-second [_])
  (world-viewport-width [_])
  (world-viewport-height [_])
  (update-viewports! [_ width height])
  (unproject-ui [_ position])
  (unproject-world [_ position]))

(defprotocol Textures
  (texture-region [_ image]))

(defprotocol DrawOnWorldViewport
  (draw-on-world-viewport! [_ f]))
