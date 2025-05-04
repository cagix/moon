(ns cdq.graphics.shape-drawer
  (:require [cdq.gdx.interop :as interop])
  (:import (com.badlogic.gdx.math MathUtils)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(declare ^:private ^ShapeDrawer this)

(defn create! [batch texture-region]
  (def this (ShapeDrawer. batch texture-region)))

(defn- degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn- set-color! [color]
  (.setColor this (interop/->color color)))

(defn ellipse [[x y] radius-x radius-y color]
  (set-color! color)
  (.ellipse this
            (float x)
            (float y)
            (float radius-x)
            (float radius-y)))

(defn filled-ellipse [[x y] radius-x radius-y color]
  (set-color! color)
  (.filledEllipse this
                  (float x)
                  (float y)
                  (float radius-x)
                  (float radius-y)))

(defn circle [[x y] radius color]
  (set-color! color)
  (.circle this
           (float x)
           (float y)
           (float radius)))

(defn filled-circle [[x y] radius color]
  (set-color! color)
  (.filledCircle this
                 (float x)
                 (float y)
                 (float radius)))

(defn arc [[center-x center-y] radius start-angle degree color]
  (set-color! color)
  (.arc this
        (float center-x)
        (float center-y)
        (float radius)
        (float (degree->radians start-angle))
        (float (degree->radians degree))))

(defn sector [[center-x center-y] radius start-angle degree color]
  (set-color! color)
  (.sector this
           (float center-x)
           (float center-y)
           (float radius)
           (float (degree->radians start-angle))
           (float (degree->radians degree))))

(defn rectangle [x y w h color]
  (set-color! color)
  (.rectangle this
              (float x)
              (float y)
              (float w)
              (float h)))

(defn filled-rectangle [x y w h color]
  (set-color! color)
  (.filledRectangle this
                    (float x)
                    (float y)
                    (float w)
                    (float h)))

(defn line [[sx sy] [ex ey] color]
  (set-color! color)
  (.line this
         (float sx)
         (float sy)
         (float ex)
         (float ey)))

(defn with-line-width [width draw-fn]
  (let [old-line-width (.getDefaultLineWidth this)]
    (.setDefaultLineWidth this (float (* width old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth this (float old-line-width))))

(defn grid [leftx bottomy gridw gridh cellw cellh color]
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
