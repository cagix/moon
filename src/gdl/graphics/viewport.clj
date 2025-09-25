(ns gdl.graphics.viewport)

(defprotocol Viewport
  (update! [_ width height {:keys [center?]}])
  (unproject [_ [x y]]))
