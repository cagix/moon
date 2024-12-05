(ns forge.app.shape-drawer
  (:require [forge.core :refer :all])
  (:import (com.badlogic.gdx.graphics Color Texture Pixmap Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(declare ^:private pixel-texture
         ^:private ^ShapeDrawer shape-drawer)

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (dispose pixmap)
    texture))

(defn create [_]
  (bind-root pixel-texture (white-pixel-texture))
  (bind-root shape-drawer (ShapeDrawer. batch
                                        (TextureRegion. ^Texture pixel-texture 1 0 1 1))))

(defn destroy [_]
  (dispose pixel-texture))

(defn- sd-color [color]
  (.setColor shape-drawer (->color color)))

(defn-impl draw-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (.ellipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)))

(defn-impl draw-filled-ellipse [[x y] radius-x radius-y color]
  (sd-color color)
  (.filledEllipse shape-drawer (float x) (float y) (float radius-x) (float radius-y)))

(defn-impl draw-circle [[x y] radius color]
  (sd-color color)
  (.circle shape-drawer (float x) (float y) (float radius)))

(defn-impl draw-filled-circle [[x y] radius color]
  (sd-color color)
  (.filledCircle shape-drawer (float x) (float y) (float radius)))

(defn-impl draw-arc [[centre-x centre-y] radius start-angle degree color]
  (sd-color color)
  (.arc shape-drawer (float centre-x) (float centre-y) (float radius) (degree->radians start-angle) (degree->radians degree)))

(defn-impl draw-sector [[centre-x centre-y] radius start-angle degree color]
  (sd-color color)
  (.sector shape-drawer (float centre-x) (float centre-y) (float radius) (degree->radians start-angle) (degree->radians degree)))

(defn-impl draw-rectangle [x y w h color]
  (sd-color color)
  (.rectangle shape-drawer (float x) (float y) (float w) (float h)))

(defn-impl draw-filled-rectangle [x y w h color]
  (sd-color color)
  (.filledRectangle shape-drawer (float x) (float y) (float w) (float h)))

(defn-impl draw-line [[sx sy] [ex ey] color]
  (sd-color color)
  (.line shape-drawer (float sx) (float sy) (float ex) (float ey)))

(defn-impl draw-grid [leftx bottomy gridw gridh cellw cellh color]
  (sd-color color)
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw-line [linex topy] [linex bottomy]))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw-line [leftx liney] [rightx liney]))))

(defn-impl with-line-width [width draw-fn]
  (let [old-line-width (.getDefaultLineWidth shape-drawer)]
    (.setDefaultLineWidth shape-drawer (float (* width old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth shape-drawer (float old-line-width))))
