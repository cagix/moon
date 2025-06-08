(ns gdl.graphics)

(defprotocol Graphics
  (delta-time [_])
  (frames-per-second [_])
  (set-cursor! [_ cursor-key])
  (texture [_ path])
  (draw-on-world-viewport! [_ f])
  (draw-tiled-map! [_ tiled-map color-setter])
  (resize-viewports! [_ width height])
  (ui-viewport-height [_])
  (handle-draws! [_ draws])
  (sprite [_ texture-path])
  (edn->sprite [_ {:keys [file sub-image-bounds]}])
  (sub-sprite [_ sprite [x y w h]])
  (sprite-sheet [_ texture-path tilew tileh])
  (sprite-sheet->sprite [_ sprite-sheet [x y]])
  (image->texture-region [_ {:keys [file sub-image-bounds]}]
                         "sub-image-bounds is optional.
                         Loads the texture and creates a texture-region out of it, in case of sub-image bounds applies the proper bounds.")
  )
