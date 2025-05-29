(ns gdl.viewport)

(defprotocol Viewport
  (update! [_ width height])
  (unproject [_ position]))
