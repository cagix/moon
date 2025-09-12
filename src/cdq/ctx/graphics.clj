(ns cdq.ctx.graphics)

(defn handle-draws!
  [{:keys [ctx/draw-fns]
    :as graphics}
   draws]
  (doseq [{k 0 :as component} draws
          :when component]
    (apply (draw-fns k) graphics (rest component))))

(defprotocol Graphics
  (dispose! [_])
  (clear! [_ [r g b a]])
  (draw-on-world-viewport! [_ f])
  (draw-tiled-map! [_ tiled-map color-setter])
  (set-cursor! [_ cursor-key])
  (delta-time [_])
  (frames-per-second [_])
  (world-viewport-width [_])
  (world-viewport-height [_])
  (camera-position [_])
  (visible-tiles [_])
  (camera-frustum [_])
  (camera-zoom [_])
  (change-zoom! [_ amount])
  (set-camera-position! [_ position])
  (texture-region [_ image])
  (update-viewports! [_ width height])
  (unproject-ui [_ position])
  (unproject-world [_ position]))
