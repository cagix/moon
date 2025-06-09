(ns clojure.gdx.graphics.pixmap)

(defprotocol Pixmap
  (set-color! [_ color])
  (draw-pixel! [_ x y]))
