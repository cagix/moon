(ns cdq.create.shape-drawer
  (:require cdq.graphics.shape-drawer
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [clojure.gdx.math.utils :refer [degree->radians]]))

; TODO earlygrey only here ... separate project ... no other dependneices?

(defn create [batch shape-drawer-texture]
  (space.earlygrey.shapedrawer.ShapeDrawer. batch
                                            (texture-region/create shape-drawer-texture 1 0 1 1)))

(defn- set-color [shape-drawer color]
  (space.earlygrey.shapedrawer.ShapeDrawer/.setColor shape-drawer (color/munge color)))

(extend-type space.earlygrey.shapedrawer.ShapeDrawer
  cdq.graphics.shape-drawer/ShapeDrawer
  (ellipse [this [x y] radius-x radius-y color]
    (set-color this color)
    (.ellipse this
              (float x)
              (float y)
              (float radius-x)
              (float radius-y)))

  (filled-ellipse [this [x y] radius-x radius-y color]
    (set-color this color)
    (.filledEllipse this
                    (float x)
                    (float y)
                    (float radius-x)
                    (float radius-y)))

  (circle [this [x y] radius color]
    (set-color this color)
    (.circle this
             (float x)
             (float y)
             (float radius)))

  (filled-circle [this [x y] radius color]
    (set-color this color)
    (.filledCircle this
                   (float x)
                   (float y)
                   (float radius)))

  (arc [this [center-x center-y] radius start-angle degree color]
    (set-color this color)
    (.arc this
          (float center-x)
          (float center-y)
          (float radius)
          (float (degree->radians start-angle))
          (float (degree->radians degree))))

  (sector [this [center-x center-y] radius start-angle degree color]
    (set-color this color)
    (.sector this
             (float center-x)
             (float center-y)
             (float radius)
             (float (degree->radians start-angle))
             (float (degree->radians degree))))

  (rectangle [this x y w h color]
    (set-color this color)
    (.rectangle this
                (float x)
                (float y)
                (float w)
                (float h)))

  (filled-rectangle [this x y w h color]
    (set-color this color)
    (.filledRectangle this
                      (float x)
                      (float y)
                      (float w)
                      (float h)))

  (line [this [sx sy] [ex ey] color]
    (set-color this color)
    (.line this
           (float sx)
           (float sy)
           (float ex)
           (float ey)))

  (with-line-width [this width draw-fn]
    (let [old-line-width (.getDefaultLineWidth this)]
      (.setDefaultLineWidth this (float (* width old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth this (float old-line-width)))))
