(ns clojure.gdx.graphics.g2d.bitmap-font
  (:require [clojure.string :as str])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont)))

(defn text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn set-scale! [^BitmapFont font scale]
  (.setScale (.getData font) (float scale)))

(defn scale-x [^BitmapFont font]
  (.scaleX (.getData font)))

(defn configure! [^BitmapFont font {:keys [scale
                                           enable-markup?
                                           use-integer-positions?]}]
  (set-scale! font scale)
  (set! (.markupEnabled (.getData font)) enable-markup?)
  (.setUseIntegerPositions font use-integer-positions?))

(defn draw! [{:keys [^BitmapFont font batch text x y target-width align wrap?]}]
  (.draw font
         batch
         text
         (float x)
         (float y)
         (float target-width)
         align
         wrap?))
