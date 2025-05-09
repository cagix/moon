(ns clojure.gdx.graphics.shape-drawer
  (:require [clojure.gdx.interop :as interop]
            [clojure.gdx.math :refer [degree->radians]])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(defn create [batch texture-region]
  (ShapeDrawer. batch texture-region))

(defn- set-color! [^ShapeDrawer shape-drawer color]
  (.setColor shape-drawer (interop/->color color)))

(defn ellipse! [^ShapeDrawer shape-drawer x y radius-x radius-y color]
  (set-color! shape-drawer color)
  (.ellipse shape-drawer
            (float x)
            (float y)
            (float radius-x)
            (float radius-y)))

(defn filled-ellipse! [^ShapeDrawer shape-drawer x y radius-x radius-y color]
  (set-color! shape-drawer color)
  (.filledEllipse shape-drawer
                  (float x)
                  (float y)
                  (float radius-x)
                  (float radius-y)))

(defn circle! [^ShapeDrawer shape-drawer x y radius color]
  (set-color! shape-drawer color)
  (.circle shape-drawer
           (float x)
           (float y)
           (float radius)))

(defn filled-circle! [^ShapeDrawer shape-drawer x y radius color]
  (set-color! shape-drawer color)
  (.filledCircle shape-drawer
                 (float x)
                 (float y)
                 (float radius)))

(defn arc! [^ShapeDrawer shape-drawer center-x center-y radius start-angle degree color]
  (set-color! shape-drawer color)
  (.arc shape-drawer
        (float center-x)
        (float center-y)
        (float radius)
        (float (degree->radians start-angle))
        (float (degree->radians degree))))

(defn sector! [^ShapeDrawer shape-drawer center-x center-y radius start-angle degree color]
  (set-color! shape-drawer color)
  (.sector shape-drawer
           (float center-x)
           (float center-y)
           (float radius)
           (float (degree->radians start-angle))
           (float (degree->radians degree))))

(defn rectangle! [^ShapeDrawer shape-drawer x y w h color]
  (set-color! shape-drawer color)
  (.rectangle shape-drawer
              (float x)
              (float y)
              (float w)
              (float h)))

(defn filled-rectangle! [^ShapeDrawer shape-drawer x y w h color]
  (set-color! shape-drawer color)
  (.filledRectangle shape-drawer
                    (float x)
                    (float y)
                    (float w)
                    (float h)))

(defn line! [^ShapeDrawer shape-drawer sx sy ex ey color]
  (set-color! shape-drawer color)
  (.line shape-drawer
         (float sx)
         (float sy)
         (float ex)
         (float ey)))

(defn with-line-width [^ShapeDrawer shape-drawer width draw-fn]
  (let [old-line-width (.getDefaultLineWidth shape-drawer)]
    (.setDefaultLineWidth shape-drawer (float (* width old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth shape-drawer (float old-line-width))))

(defn grid! [shape-drawer leftx bottomy gridw gridh cellw cellh color]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (line! shape-drawer [linex topy] [linex bottomy] color))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (line! shape-drawer [leftx liney] [rightx liney] color))))
