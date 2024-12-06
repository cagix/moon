(ns clojure.gdx.graphics.shape-drawer
  (:import (com.badlogic.gdx.graphics Color)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn create [batch texture-region]
  (ShapeDrawer. batch texture-region))

(defn default-line-width [^ShapeDrawer sd]
  (.getDefaultLineWidth sd))

(defn set-default-line-width [^ShapeDrawer sd width]
  (.setDefaultLineWidth sd (float width)))

(defn set-color [^ShapeDrawer sd ^Color color]
  (.setColor sd color))

(defn ellipse [^ShapeDrawer sd [x y] radius-x radius-y]
  (.ellipse sd
            (float x)
            (float y)
            (float radius-x)
            (float radius-y)))

(defn filled-ellipse [^ShapeDrawer sd [x y] radius-x radius-y]
  (.filledEllipse sd
                  (float x)
                  (float y)
                  (float radius-x)
                  (float radius-y)))

(defn circle [^ShapeDrawer sd [x y] radius]
  (.circle sd
           (float x)
           (float y)
           (float radius)))

(defn filled-circle [^ShapeDrawer sd [x y] radius]
  (.filledCircle sd
                 (float x)
                 (float y)
                 (float radius)))

(defn arc [^ShapeDrawer sd [center-x center-y] radius start-angle degree]
  (.arc sd
        (float center-x)
        (float center-y)
        (float radius)
        (float start-angle)
        (float degree)))

(defn sector [^ShapeDrawer sd [center-x center-y] radius start-angle radians]
  (.sector sd
           (float center-x)
           (float center-y)
           (float radius)
           (float start-angle)
           (float radians)))

(defn rectangle [^ShapeDrawer sd x y w h]
  (.rectangle sd
              (float x)
              (float y)
              (float w)
              (float h)))

(defn filled-rectangle [^ShapeDrawer sd x y w h]
  (.filledRectangle sd
                    (float x)
                    (float y)
                    (float w)
                    (float h)))

(defn line [^ShapeDrawer sd [sx sy] [ex ey]]
  (.line sd
         (float sx)
         (float sy)
         (float ex)
         (float ey)))
