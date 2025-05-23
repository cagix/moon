(ns cdq.c)

(defprotocol Disposable
  (dispose! [_]))

(defprotocol Textures
  (texture [_ path])
  (all-textures [_]))

(defprotocol Sounds
  (sound [_ path])
  (all-sounds [_]))

(defprotocol Graphics
  (delta-time [_])
  (clear-screen! [_])
  (handle-draws! [_ draws])
  (world-mouse-position [_])
  (ui-mouse-position [_])
  (camera-position [_])
  (inc-zoom! [_ amount])
  (camera-frustum [_])
  (visible-tiles [_])
  (camera-zoom [_])
  (pixels->world-units [_ pixels])
  (sprite [_ texture-path])
  (sub-sprite [_ sprite [x y w h]])
  (sprite-sheet [_ texture-path tilew tileh])
  (sprite-sheet->sprite [_ sprite [x y]])
  (set-cursor! [_ cursor-key])
  (world-viewport-width [_])
  (world-viewport-height [_])
  (ui-viewport-width [_])
  (ui-viewport-height [_])
  (draw-on-world-viewport! [_ fns])
  (draw-tiled-map! [_ tiled-map color-setter])
  (set-camera-position! [_ position])
  (update-viewports! [_]))

(defprotocol Stage
  (get-actor [_ id])
  (find-actor-by-name [_ name])
  (add-actor! [_ actor])
  (mouseover-actor [_])
  (reset-actors! [_ actors])
  (draw-stage! [_])
  (update-stage! [_]))

(defprotocol Input
  (button-just-pressed? [_ button])
  (key-pressed? [_ key])
  (key-just-pressed? [_ key]))
