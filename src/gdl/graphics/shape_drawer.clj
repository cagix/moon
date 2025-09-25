(ns gdl.graphics.shape-drawer)

(defprotocol ShapeDrawer
  (set-color! [_ color])
  (with-line-width [_ width draw-fn])
  (arc! [_ center-x center-y radius start-radians radians])
  (circle! [_ x y radius])
  (ellipse! [_ x y radius-x radius-y])
  (filled-circle! [_ x y radius])
  (filled-ellipse! [_ x y radius-x radius-y])
  (filled-rectangle! [_ x y w h])
  (line! [_ sx sy ex ey])
  (rectangle! [_ x y w h])
  (sector! [_ center-x center-y radius start-radians radians]))
