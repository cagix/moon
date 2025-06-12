(ns gdl.graphics.viewport)

(defprotocol Viewport
  (unproject [_ position]))
