(ns gdl.graphics)

(defprotocol Cursors
  (cursor [_ path [hotspot-x hotspot-y]]))

(defprotocol Graphics
  (clear-screen! [_ color])
  (delta-time [_])
  (frames-per-second [_])
  (set-cursor! [_ cursor-key])
  (texture [_ path])
  (draw-on-world-viewport! [_ f])
  (draw-tiled-map! [_ tiled-map color-setter])
  (resize-viewports! [_ width height])
  (ui-viewport-height [_])
  (handle-draws! [_ draws])
  (image->texture-region [_ image]
                         "image is `:image/file` (string) & `:image/bounds` `[x y w h]` (optional).

                         Loads the texture and creates a texture-region out of it, in case of sub-image bounds applies the proper bounds."))
