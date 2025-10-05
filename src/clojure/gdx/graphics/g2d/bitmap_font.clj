(ns clojure.gdx.graphics.g2d.bitmap-font
  (:require [clojure.gdx.utils.align :as align]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont
                                          BitmapFont$BitmapFontData)))

(defn line-height
  "Returns the line height, which is the distance from one line of text to the next."
  [^BitmapFont font]
  (.getLineHeight font))

(defn data
  "Gets the underlying {@link BitmapFontData} for this BitmapFont."
  ^BitmapFont$BitmapFontData [^BitmapFont font]
  (.getData font))

(defn set-use-integer-positions!
  "Specifies whether to use integer positions. Default is to use them so filtering doesn't kick in as badly."
  [^BitmapFont font boolean]
  (.setUseIntegerPositions font boolean))

(defn set-scale!
  "Scales the font by the specified amount in both directions.
  throws IllegalArgumentException if scaleX or scaleY is zero."
  [font scale-xy]
  (.setScale (data font) scale-xy))

(defn text-height [font text]
  (-> text
      (str/split #"\n")
      count
      (* (line-height font))))

(defn enable-markup!
  [font boolean]
  (set! (.markupEnabled (data font)) boolean))

(defn scale-x
  [font]
  (.scaleX (data font)))

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
