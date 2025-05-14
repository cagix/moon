(ns cdq.graphics)

(defprotocol Graphics
  (mouse-position [_])
  (world-mouse-position [_])
  (pixels->world-units [_ pixels])
  (draw-image [_ image position])
  (draw-rotated-centered [_ image rotation [x y]])

  (draw-text [_ {:keys [font scale x y text h-align up?]}]
             "font, h-align, up? and scale are optional.
             h-align one of: :center, :left, :right. Default :center.
             up? renders the font over y, otherwise under.
             scale will multiply the drawn text size with the scale.")

  (draw-ellipse [_ [x y] radius-x radius-y color])

  (draw-filled-ellipse [_ [x y] radius-x radius-y color])

  (draw-circle [_ [x y] radius color])

  (draw-filled-circle [_ [x y] radius color])

  (draw-arc [_ [center-x center-y] radius start-angle degree color])

  (draw-sector [_ [center-x center-y] radius start-angle degree color])

  (draw-rectangle [_ x y w h color])

  (draw-filled-rectangle [_ x y w h color])

  (draw-line [_ [sx sy] [ex ey] color])

  (with-line-width [_ width draw-fn])

  (draw-grid [_ leftx bottomy gridw gridh cellw cellh color])

  (set-cursor! [_ cursor-key])

  (sprite [_ texture])
  (sub-sprite [_ sprite [x y w h]])
  (sprite-sheet [_ texture tilew tileh])
  (from-sheet [_ sprite-sheet [x y]]))

(defn draw-centered [graphics image position]
  (draw-rotated-centered graphics image 0 position))
