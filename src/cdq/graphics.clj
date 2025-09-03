(ns cdq.graphics)

(defprotocol Graphics
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
  (handle-draws! [_ draws]))

(defmulti draw!
  (fn [[k] _graphics]
    k))

(defn handle-draws! [graphics draws]
  (doseq [component draws
          :when component]
    (draw! component graphics)))
