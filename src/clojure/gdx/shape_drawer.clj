(ns clojure.gdx.shape-drawer
  (:require [clojure.color :as color]
            [clojure.math :as math]))

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

; => TODO THIS IS PART OF MACRO ! THE API IS MINIMAL !
(defmacro with-line-width [shape-drawer width & exprs]
  `(let [old-line-width# (.getDefaultLineWidth ~shape-drawer)]
     (.setDefaultLineWidth ~shape-drawer (* ~width old-line-width#))
     ~@exprs
     (.setDefaultLineWidth ~shape-drawer old-line-width#)))

(defn create [batch texture-region]
  (space.earlygrey.shapedrawer.ShapeDrawer. batch texture-region))

(extend-type space.earlygrey.shapedrawer.ShapeDrawer
  ShapeDrawer
  (arc! [this [center-x center-y] radius start-angle degree color]
    (.setColor this (color/float-bits color))
    (.arc this
          center-x
          center-y
          radius
          (math/to-radians start-angle)
          (math/to-radians degree)) )

  (circle! [this [x y] radius color]
    (.setColor this (color/float-bits color))
    (.circle this x y radius))

  (ellipse! [this [x y] radius-x radius-y color]
    (.setColor this (color/float-bits color))
    (.ellipse this x y radius-x radius-y))

  (filled-ellipse! [this [x y] radius-x radius-y color]
    (.setColor this (color/float-bits color))
    (.filledEllipse this x y radius-x radius-y) )

  (filled-circle! [this [x y] radius color]
    (.setColor this (color/float-bits color))
    (.filledCircle this (float x) (float y) (float radius)))

  (filled-rectangle! [this x y w h color]
    (.setColor this (color/float-bits color))
    (.filledRectangle this (float x) (float y) (float w) (float h)))

  (line! [this [sx sy] [ex ey] color]
    (.setColor this (color/float-bits color))
    (.line this (float sx) (float sy) (float ex) (float ey)))

  (rectangle! [this x y w h color]
    (.setColor this (color/float-bits color))
    (.rectangle this x y w h) )

  (sector! [this [center-x center-y] radius start-angle degree color]
    (.setColor this (color/float-bits color))
    (.sector this
             center-x
             center-y
             radius
             (math/to-radians start-angle)
             (math/to-radians degree))))
