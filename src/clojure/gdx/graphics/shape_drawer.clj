(ns clojure.gdx.graphics.shape-drawer
  (:import (com.badlogic.gdx.graphics Color)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn create [batch texture-region]
  (ShapeDrawer. batch texture-region))

(defn default-line-width [sd]
  (ShapeDrawer/.getDefaultLineWidth sd))

(defn set-default-line-width [sd width]
  (ShapeDrawer/.setDefaultLineWidth sd (float width)))

(defn set-color [sd color]
  (ShapeDrawer/.setColor sd ^Color color))

(defn ellipse [sd x y radius-x radius-y]
  (ShapeDrawer/.ellipse sd
                        (float x)
                        (float y)
                        (float radius-x)
                        (float radius-y)))

(defn filled-ellipse [sd x y radius-x radius-y]
  (ShapeDrawer/.filledEllipse sd
                              (float x)
                              (float y)
                              (float radius-x)
                              (float radius-y)))

(defn circle [sd x y radius]
  (ShapeDrawer/.circle sd
                       (float x)
                       (float y)
                       (float radius)))

(defn filled-circle [sd x y radius]
  (ShapeDrawer/.filledCircle sd
                             (float x)
                             (float y)
                             (float radius)))

(defn arc [sd center-x center-y radius start-angle radians]
  (ShapeDrawer/.arc sd
                    (float center-x)
                    (float center-y)
                    (float radius)
                    (float start-angle)
                    (float radians)))

(defn sector [sd center-x center-y radius start-angle radians]
  (ShapeDrawer/.sector sd
                       (float center-x)
                       (float center-y)
                       (float radius)
                       (float start-angle)
                       (float radians)))

(defn rectangle [sd x y w h]
  (ShapeDrawer/.rectangle sd
                          (float x)
                          (float y)
                          (float w)
                          (float h)))

(defn filled-rectangle [sd x y w h]
  (ShapeDrawer/.filledRectangle sd
                                (float x)
                                (float y)
                                (float w)
                                (float h)))

(defn line [sd sx sy ex ey]
  (ShapeDrawer/.line sd
                     (float sx)
                     (float sy)
                     (float ex)
                     (float ey)))
