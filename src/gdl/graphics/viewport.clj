(ns gdl.graphics.viewport)

(defprotocol Viewport
  (camera [_])
  (world-width [_])
  (world-height [_])
  (update! [_ width height {:keys [center?]}])
  (unproject [_ [x y]]))
