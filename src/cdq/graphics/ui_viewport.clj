(ns cdq.graphics.ui-viewport)

(defprotocol UIViewport
  (unproject [_ position])
  (update! [_ width height]))
