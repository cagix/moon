(ns forge.app.shape-drawer
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.shape-drawer :as sd]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [forge.utils :refer [bind-root]]
            [forge.core :refer :all]))

(declare ^:private pixel-texture
         ^:private sd)

(defn- white-pixel-texture []
  (let [pixmap (doto (g/pixmap 1 1)
                 (.setColor color/white)
                 (.drawPixel 0 0))
        texture (g/texture pixmap)]
    (dispose pixmap)
    texture))

(defn create [_]
  (bind-root pixel-texture (white-pixel-texture))
  (bind-root sd (sd/create batch (g/texture-region pixel-texture 1 0 1 1))))

(defn destroy [_]
  (dispose pixel-texture))

(defn- sd-color [color]
  (sd/set-color sd (->color color)))

(defn-impl draw-ellipse [position radius-x radius-y color]
  (sd-color color)
  (sd/ellipse sd position radius-x radius-y))

(defn-impl draw-filled-ellipse [position radius-x radius-y color]
  (sd-color color)
  (sd/filled-ellipse sd position radius-x radius-y))

(defn-impl draw-circle [position radius color]
  (sd-color color)
  (sd/circle sd position radius))

(defn-impl draw-filled-circle [position radius color]
  (sd-color color)
  (sd/filled-circle sd position radius))

(defn-impl draw-arc [center radius start-angle degree color]
  (sd-color color)
  (sd/arc sd
          center
          radius
          (degree->radians start-angle)
          (degree->radians degree)))

(defn-impl draw-sector [center radius start-angle degree color]
  (sd-color color)
  (sd/sector sd
             center
             radius
             (degree->radians start-angle)
             (degree->radians degree)))

(defn-impl draw-rectangle [x y w h color]
  (sd-color color)
  (sd/rectangle sd x y w h))

(defn-impl draw-filled-rectangle [x y w h color]
  (sd-color color)
  (sd/filled-rectangle sd x y w h))

(defn-impl draw-line [start end color]
  (sd-color color)
  (sd/line sd start end))

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
  (let [old-line-width (sd/default-line-width sd)]
    (sd/set-default-line-width sd (* width old-line-width))
    (draw-fn)
    (sd/set-default-line-width sd old-line-width)))
