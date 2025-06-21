(ns gdl.graphics.viewport)

(defprotocol Viewport
  (unproject [_ x y])
  (update! [_ width height center-camera?]))
