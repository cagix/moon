(ns cdq.ctx.graphics)

(defprotocol Graphics
  (dispose! [_])
  (clear-screen! [_ color])
  (delta-time [_])
  (frames-per-second [_])
  (set-cursor! [_ cursor-key])
  (texture [_ path]
           "Returns the already loaded texture whit given file path.")
  (draw-on-world-viewport! [_ f])
  (draw-tiled-map! [_ tiled-map color-setter])
  (resize-viewports! [_ width height])
  (ui-viewport-height [_])
  (image->texture-region [_ image]
                         "image is `:image/file` (string) & `:image/bounds` `[x y w h]` (optional).

                         Loads the texture and creates a texture-region out of it, in case of sub-image bounds applies the proper bounds.")
  (handle-draws! [_ draws])
  (zoom-in! [_ amount])
  (zoom-out! [_ amount])
  (set-camera-position! [_ position]))
