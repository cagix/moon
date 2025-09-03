(ns cdq.gdx.graphics.shape-drawer
  (:import (com.badlogic.gdx.graphics Color)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn create [batch texture-region]
  (ShapeDrawer. batch texture-region))

(defn set-color! [shape-drawer color]
  (ShapeDrawer/.setColor shape-drawer ^Color color))

(defn with-line-width [^ShapeDrawer this width draw-fn]
  (let [old-line-width (.getDefaultLineWidth this)]
    (.setDefaultLineWidth this (float (* width old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth this (float old-line-width))))

(defn arc! [shape-drawer center-x center-y radius start-radians radians]
  (ShapeDrawer/.arc shape-drawer
                    (float center-x)
                    (float center-y)
                    (float radius)
                    (float start-radians)
                    (float radians)))

(defn circle! [shape-drawer x y radius]
  (ShapeDrawer/.circle shape-drawer
                       (float x)
                       (float y)
                       (float radius)))

(defn ellipse! [shape-drawer x y radius-x radius-y]
  (ShapeDrawer/.ellipse shape-drawer
                        (float x)
                        (float y)
                        (float radius-x)
                        (float radius-y)))

(defn filled-circle! [shape-drawer x y radius]
  (ShapeDrawer/.filledCircle shape-drawer
                             (float x)
                             (float y)
                             (float radius)))

(defn filled-ellipse! [shape-drawer x y radius-x radius-y]
  (ShapeDrawer/.filledEllipse shape-drawer
                              (float x)
                              (float y)
                              (float radius-x)
                              (float radius-y)))

(defn filled-rectangle! [shape-drawer x y w h]
  (ShapeDrawer/.filledRectangle shape-drawer
                                (float x)
                                (float y)
                                (float w)
                                (float h)))

(defn line! [shape-drawer sx sy ex ey]
  (ShapeDrawer/.line shape-drawer
                     (float sx)
                     (float sy)
                     (float ex)
                     (float ey)))

(defn rectangle! [shape-drawer x y w h]
  (ShapeDrawer/.rectangle shape-drawer
                          (float x)
                          (float y)
                          (float w)
                          (float h)))

(defn sector! [shape-drawer center-x center-y radius start-radians radians]
  (ShapeDrawer/.sector shape-drawer
                       (float center-x)
                       (float center-y)
                       (float radius)
                       (float start-radians)
                       (float radians)))
