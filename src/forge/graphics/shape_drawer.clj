(ns forge.graphics.shape-drawer
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.math :refer [degree->radians]]
            [forge.graphics.color])
  (:import (com.badlogic.gdx.graphics Texture Pixmap Pixmap$Format)
           (com.badlogic.gdx.graphics.g2d TextureRegion)))

(defn white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor color/white)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn create [batch sd-texture]
  (sd/create batch (TextureRegion. sd-texture 1 0 1 1)))

(defn set-color [sd color]
  (sd/set-color sd (forge.graphics.color/munge color)))

(defn ellipse [sd [x y] radius-x radius-y]
  (sd/ellipse sd x y radius-x radius-y))

(defn filled-ellipse [sd [x y] radius-x radius-y]
  (sd/filled-ellipse sd x y radius-x radius-y))

(defn circle [sd [x y] radius]
  (sd/circle sd x y radius))

(defn filled-circle [sd [x y] radius]
  (sd/filled-circle sd x y radius))

(defn arc [sd [centre-x centre-y] radius start-angle degree]
  (sd/arc sd centre-x centre-y radius (degree->radians start-angle) (degree->radians degree)))

(defn sector [sd [centre-x centre-y] radius start-angle degree]
  (sd/sector sd centre-x centre-y radius (degree->radians start-angle) (degree->radians degree)))

(defn rectangle [sd x y w h]
  (sd/rectangle sd x y w h))

(defn filled-rectangle [sd x y w h]
  (sd/filled-rectangle sd x y w h))

(defn line [sd [sx sy] [ex ey]]
  (sd/line sd sx sy ex ey))

(defn grid [sd leftx bottomy gridw gridh cellw cellh]
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

(defn with-line-width [sd width draw-fn]
  (let [old-line-width (sd/default-line-width sd)]
    (sd/set-default-line-width sd (* width old-line-width))
    (draw-fn)
    (sd/set-default-line-width sd old-line-width)))
