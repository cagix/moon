(ns cdq.graphics.shape-drawer
  (:require [cdq.gdx.interop :as interop])
  (:import (com.badlogic.gdx.math MathUtils)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn create [batch texture-region]
  (ShapeDrawer. batch texture-region))

(defn- degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn- set-color! [^ShapeDrawer this color]
  (.setColor this (interop/->color color)))

(defn ellipse [^ShapeDrawer this [x y] radius-x radius-y color]
  (set-color! this color)
  (.ellipse this
            (float x)
            (float y)
            (float radius-x)
            (float radius-y)))

(defn filled-ellipse [^ShapeDrawer this [x y] radius-x radius-y color]
  (set-color! this color)
  (.filledEllipse this
                  (float x)
                  (float y)
                  (float radius-x)
                  (float radius-y)))

(defn circle [^ShapeDrawer this [x y] radius color]
  (set-color! this color)
  (.circle this
           (float x)
           (float y)
           (float radius)))

(defn filled-circle [^ShapeDrawer this [x y] radius color]
  (set-color! this color)
  (.filledCircle this
                 (float x)
                 (float y)
                 (float radius)))

(defn arc [^ShapeDrawer this [center-x center-y] radius start-angle degree color]
  (set-color! this color)
  (.arc this
        (float center-x)
        (float center-y)
        (float radius)
        (float (degree->radians start-angle))
        (float (degree->radians degree))))

(defn sector [^ShapeDrawer this [center-x center-y] radius start-angle degree color]
  (set-color! this color)
  (.sector this
           (float center-x)
           (float center-y)
           (float radius)
           (float (degree->radians start-angle))
           (float (degree->radians degree))))

(defn rectangle [^ShapeDrawer this x y w h color]
  (set-color! this color)
  (.rectangle this
              (float x)
              (float y)
              (float w)
              (float h)))

(defn filled-rectangle [^ShapeDrawer this x y w h color]
  (set-color! this color)
  (.filledRectangle this
                    (float x)
                    (float y)
                    (float w)
                    (float h)))

(defn line [^ShapeDrawer this [sx sy] [ex ey] color]
  (set-color! this color)
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

(defn grid [this leftx bottomy gridw gridh cellw cellh color]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (line this [linex topy] [linex bottomy] color))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (line this [leftx liney] [rightx liney] color))))
