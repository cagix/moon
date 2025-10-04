(ns clojure.graphics.bitmap-font)

(defprotocol BitmapFont
  (configure! [_ {:keys [scale
                         enable-markup?
                         use-integer-positions?]}])
  (draw! [_ batch {:keys [scale text x y up? h-align target-width wrap?]}]))
