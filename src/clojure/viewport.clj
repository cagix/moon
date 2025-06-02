(ns clojure.viewport)

(defprotocol Viewport
  (resize! [_ width height])
  (unproject [_ position]))
