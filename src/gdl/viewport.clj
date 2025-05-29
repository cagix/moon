(ns gdl.viewport)

(defprotocol Viewport
  (unproject [_ position]))
