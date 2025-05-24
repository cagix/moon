(ns clojure.gdx.graphics.g2d.bitmap-font
  (:require [clojure.gdx.interop :as interop]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont)))

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- set-scale! [^BitmapFont font scale]
  (.setScale (.getData font) (float scale)))

(defn- scale-x [^BitmapFont font]
  (.scaleX (.getData font)))

(defn draw! [^BitmapFont font batch {:keys [scale x y text h-align up?]}]
  (let [old-scale (float (scale-x font))
        target-width (float 0)
        wrap? false]
    (set-scale! font (* old-scale (float scale)))
    (.draw font
           (:java-object batch)
           text
           (float x)
           (float (+ y (if up? (text-height font text) 0)))
           target-width
           (interop/k->align (or h-align :center))
           wrap?)
    (set-scale! font old-scale)))

(defn configure! [^BitmapFont font {:keys [scale
                                           enable-markup?
                                           use-integer-positions?]}]
  (set-scale! font scale)
  (set! (.markupEnabled (.getData font)) enable-markup?)
  (.setUseIntegerPositions font use-integer-positions?)
  font)
