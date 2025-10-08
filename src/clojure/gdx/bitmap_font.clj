(ns clojure.gdx.bitmap-font)

(defprotocol BitmapFont
  (draw! [_ batch {:keys [scale text x y up? h-align target-width wrap?]}]))
