(ns clojure.graphics.bitmap-font)

(defprotocol BitmapFont
  (draw! [font
          batch
          {:keys [scale text x y up? h-align target-width wrap?]}]
         "font, h-align, up? and scale are optional.
         h-align one of: :center, :left, :right. Default :center.
         up? renders the font over y, otherwise under.
         scale will multiply the drawn text size with the scale."))
