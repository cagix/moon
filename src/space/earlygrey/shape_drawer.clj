(ns space.earlygrey.shape-drawer
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defn create [batch texture-region]
  (ShapeDrawer. batch texture-region))

(defprotocol PShapeDrawer
  (set-color! [_ color-float-bits])
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

(extend-type ShapeDrawer
  PShapeDrawer
  (set-color! [this color-float-bits]
    (.setColor this (float color-float-bits)))

  (with-line-width [this width draw-fn]
    (let [old-line-width (.getDefaultLineWidth this)]
      (.setDefaultLineWidth this (float (* width old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth this (float old-line-width))))

  (arc! [this center-x center-y radius start-radians radians]
    (.arc this
          (float center-x)
          (float center-y)
          (float radius)
          (float start-radians)
          (float radians)))

  (circle! [this x y radius]
    (.circle this
             (float x)
             (float y)
             (float radius)))

  (ellipse! [this x y radius-x radius-y]
    (.ellipse this
              (float x)
              (float y)
              (float radius-x)
              (float radius-y)))

  (filled-circle! [this x y radius]
    (.filledCircle this
                   (float x)
                   (float y)
                   (float radius)))

  (filled-ellipse! [this x y radius-x radius-y]
    (.filledEllipse this
                    (float x)
                    (float y)
                    (float radius-x)
                    (float radius-y)))

  (filled-rectangle! [this x y w h]
    (.filledRectangle this
                      (float x)
                      (float y)
                      (float w)
                      (float h)))

  (line! [this sx sy ex ey]
    (.line this
           (float sx)
           (float sy)
           (float ex)
           (float ey)))

  (rectangle! [this x y w h]
    (.rectangle this
                (float x)
                (float y)
                (float w)
                (float h)))

  (sector! [this center-x center-y radius start-radians radians]
    (.sector this
             (float center-x)
             (float center-y)
             (float radius)
             (float start-radians)
             (float radians))))
