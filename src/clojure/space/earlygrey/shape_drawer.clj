(ns clojure.space.earlygrey.shape-drawer
  (:require [clojure.gdx.math.math-utils :as math-utils]
            [clojure.gdx.interop :as interop])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defn create [batch texture-region]
  (ShapeDrawer. batch texture-region))

(defn set-color! [^ShapeDrawer this color]
  (.setColor this (interop/->color color)))

(defn ellipse! [^ShapeDrawer this x y radius-x radius-y]
  (.ellipse this
            (float x)
            (float y)
            (float radius-x)
            (float radius-y)))

(defn filled-ellipse! [^ShapeDrawer this x y radius-x radius-y]
  (.filledEllipse this
                  (float x)
                  (float y)
                  (float radius-x)
                  (float radius-y)))

(defn circle! [^ShapeDrawer this x y radius]
  (.circle this
           (float x)
           (float y)
           (float radius)))

(defn filled-circle! [^ShapeDrawer this x y radius]
  (.filledCircle this
                 (float x)
                 (float y)
                 (float radius)))

(defn arc! [^ShapeDrawer this center-x center-y radius start-angle degree]
  (.arc this
        (float center-x)
        (float center-y)
        (float radius)
        (float (math-utils/degree->radians start-angle))
        (float (math-utils/degree->radians degree))))

(defn sector! [^ShapeDrawer this center-x center-y radius start-angle degree]
  (.sector this
           (float center-x)
           (float center-y)
           (float radius)
           (float (math-utils/degree->radians start-angle))
           (float (math-utils/degree->radians degree))))

(defn rectangle! [^ShapeDrawer this x y w h]
  (.rectangle this
              (float x)
              (float y)
              (float w)
              (float h)))

(defn filled-rectangle! [^ShapeDrawer this x y w h]
  (.filledRectangle this
                    (float x)
                    (float y)
                    (float w)
                    (float h)))

(defn line! [^ShapeDrawer this sx sy ex ey]
  (.line this
         (float sx)
         (float sy)
         (float ex)
         (float ey)))

(defn with-line-width [^ShapeDrawer this width draw-fn]
  (let [old-line-width (.getDefaultLineWidth this)]
    (.setDefaultLineWidth this (float (* width old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth this (float old-line-width))))
