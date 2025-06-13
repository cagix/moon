(ns clojure.gdx.shape-drawer
  (:require [gdx.graphics.color :as color])
  (:import (space.earlygrey.shapedrawer ShapeDrawer)))

(def ^:private degrees-to-radians (float (/ Math/PI 180)))

(defn- degree->radians [degree]
  (* degrees-to-radians (float degree)))

(defn create [batch texture-region]
  (ShapeDrawer. batch texture-region))

(defn- set-color! [shape-drawer color]
  (ShapeDrawer/.setColor shape-drawer (color/->obj color)))

(defn ellipse! [shape-drawer [x y] radius-x radius-y color]
  (set-color! shape-drawer color)
  (ShapeDrawer/.ellipse shape-drawer
                        (float x)
                        (float y)
                        (float radius-x)
                        (float radius-y)))

(defn filled-ellipse! [shape-drawer [x y] radius-x radius-y color]
  (set-color! shape-drawer color)
  (ShapeDrawer/.filledEllipse shape-drawer
                              (float x)
                              (float y)
                              (float radius-x)
                              (float radius-y)))

(defn circle! [shape-drawer [x y] radius color]
  (set-color! shape-drawer color)
  (ShapeDrawer/.circle shape-drawer
                       (float x)
                       (float y)
                       (float radius)))

(defn filled-circle! [shape-drawer [x y] radius color]
  (set-color! shape-drawer color)
  (ShapeDrawer/.filledCircle shape-drawer
                             (float x)
                             (float y)
                             (float radius)))

(defn rectangle! [shape-drawer x y w h color]
  (set-color! shape-drawer color)
  (ShapeDrawer/.rectangle shape-drawer
                          (float x)
                          (float y)
                          (float w)
                          (float h)))

(defn filled-rectangle! [shape-drawer x y w h color]
  (set-color! shape-drawer color)
  (ShapeDrawer/.filledRectangle shape-drawer
                                (float x)
                                (float y)
                                (float w)
                                (float h)))

(defn arc! [shape-drawer [center-x center-y] radius start-angle degree color]
  (set-color! shape-drawer color)
  (ShapeDrawer/.arc shape-drawer
                    (float center-x)
                    (float center-y)
                    (float radius)
                    (float (degree->radians start-angle))
                    (float (degree->radians degree))))

(defn sector! [shape-drawer [center-x center-y] radius start-angle degree color]
  (set-color! shape-drawer color)
  (ShapeDrawer/.sector shape-drawer
                       (float center-x)
                       (float center-y)
                       (float radius)
                       (float (degree->radians start-angle))
                       (float (degree->radians degree))))

(defn line! [shape-drawer [sx sy] [ex ey] color]
  (set-color! shape-drawer color)
  (ShapeDrawer/.line shape-drawer
                     (float sx)
                     (float sy)
                     (float ex)
                     (float ey)))

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
      (line! shape-drawer [leftx liney] [rightx liney] color ))))

(defn with-line-width [^ShapeDrawer this width draw-fn]
  (let [old-line-width (.getDefaultLineWidth this)]
    (.setDefaultLineWidth this (float (* width old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth this (float old-line-width))))
