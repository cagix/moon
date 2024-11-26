(ns forge.graphics.shape-drawer
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.math :refer [degree->radians]]
            [forge.graphics.color])
  (:import (com.badlogic.gdx.graphics Texture Pixmap Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d TextureRegion)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defn white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor color/white)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn create [batch sd-texture]
  (ShapeDrawer. batch (TextureRegion. sd-texture 1 0 1 1)))

(defn set-color [^ShapeDrawer sd color]
  (.setColor sd (forge.graphics.color/munge color)))

(defn ellipse [^ShapeDrawer sd [x y] radius-x radius-y]
  (.ellipse sd (float x) (float y) (float radius-x) (float radius-y)))

(defn filled-ellipse [^ShapeDrawer sd [x y] radius-x radius-y]
  (.filledEllipse sd (float x) (float y) (float radius-x) (float radius-y)))

(defn circle [^ShapeDrawer sd [x y] radius]
  (.circle sd (float x) (float y) (float radius)))

(defn filled-circle [^ShapeDrawer sd [x y] radius]
  (.filledCircle sd (float x) (float y) (float radius)))

(defn arc [^ShapeDrawer sd [centre-x centre-y] radius start-angle degree]
  (.arc sd centre-x centre-y radius (degree->radians start-angle) (degree->radians degree)))

(defn sector [^ShapeDrawer sd [centre-x centre-y] radius start-angle degree]
  (.sector sd centre-x centre-y radius (degree->radians start-angle) (degree->radians degree)))

(defn rectangle [^ShapeDrawer sd x y w h]
  (.rectangle sd x y w h))

(defn filled-rectangle [^ShapeDrawer sd x y w h]
  (.filledRectangle sd (float x) (float y) (float w) (float h)) )

(defn line [^ShapeDrawer sd [sx sy] [ex ey]]
  (.line sd (float sx) (float sy) (float ex) (float ey)))

(defn grid [^ShapeDrawer sd leftx bottomy gridw gridh cellw cellh]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (line sd [linex topy] [linex bottomy]))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (line sd [leftx liney] [rightx liney]))))

(defn with-line-width [^ShapeDrawer sd width draw-fn]
  (let [old-line-width (.getDefaultLineWidth sd)]
    (.setDefaultLineWidth sd (float (* (float width) old-line-width)))
    (draw-fn)
    (.setDefaultLineWidth sd (float old-line-width))))
