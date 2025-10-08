(ns clojure.gdx.shape-drawer)

(defprotocol ShapeDrawer
  (arc! [_ [center-x center-y] radius start-angle degree color])
  (circle! [_ [x y] radius color])
  (ellipse! [_ [x y] radius-x radius-y color])
  (filled-ellipse! [_ [x y] radius-x radius-y color])
  (filled-circle! [_ [x y] radius color])
  (filled-rectangle! [_ x y w h color])
  (line! [_ [sx sy] [ex ey] color])
  (rectangle! [_ x y w h color])
  (sector! [_ [center-x center-y] radius start-angle degree color]))

(defmacro with-line-width [shape-drawer width & exprs]
  `(let [old-line-width# (.getDefaultLineWidth ~shape-drawer)]
     (.setDefaultLineWidth ~shape-drawer (* ~width old-line-width#))
     ~@exprs
     (.setDefaultLineWidth ~shape-drawer old-line-width#)))
