(ns clojure.gdx.graphics.g2d.bitmap-font
  (:require [com.badlogic.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [com.badlogic.gdx.graphics.g2d.bitmap-font.data :as data]
            [com.badlogic.gdx.utils.align :as align]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont)))

(defn- set-scale! [font scale-xy]
  (data/set-scale! (bitmap-font/data font) scale-xy))

(defn- text-height [font text]
  (-> text
      (str/split #"\n")
      count
      (* (bitmap-font/line-height font))))

(defn- scale-x
  [font]
  (data/scale-x (bitmap-font/data font)))

(defn draw! [^BitmapFont font batch {:keys [scale text x y up? h-align target-width wrap?]}]
  {:pre [(or (nil? h-align)
             (contains? align/k->value h-align))]}
  (let [old-scale (scale-x font)]
    (set-scale! font (* old-scale scale))
    (.draw font
           batch
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           (float target-width)
           (get align/k->value (or h-align :center))
           wrap?)
    (set-scale! font old-scale)))
