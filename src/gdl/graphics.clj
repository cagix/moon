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
  (edn->sprite [_ {:keys [file sub-image-bounds]}])
  (image->texture-region [_ {:keys [file sub-image-bounds]}]
                         "sub-image-bounds is optional. `[x y w h]`
                         Loads the texture and creates a texture-region out of it, in case of sub-image bounds applies the proper bounds.")
  )
