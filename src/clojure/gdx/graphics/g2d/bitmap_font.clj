(ns clojure.gdx.graphics.g2d.bitmap-font
  (:require [com.badlogic.gdx.graphics.g2d.bitmap-font :as bitmap-font]
            [com.badlogic.gdx.utils.align :as align]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont
                                          BitmapFont$BitmapFontData)))

(def set-use-integer-positions! bitmap-font/set-use-integer-positions!)

; used
(defn set-scale!
  "Scales the font by the specified amount in both directions.
  throws IllegalArgumentException if scaleX or scaleY is zero."
  [font scale-xy]
  (.setScale (bitmap-font/data font) scale-xy))

(defn- text-height [font text]
  (-> text
      (str/split #"\n")
      count
      (* (bitmap-font/line-height font))))

; used
(defn enable-markup!
  [font boolean]
  (set! (.markupEnabled (bitmap-font/data font)) boolean))

(defn scale-x
  [font]
  (.scaleX (bitmap-font/data font)))

; used
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
