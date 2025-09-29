(ns com.badlogic.gdx.graphics.g2d.bitmap-font
  (:require [clojure.string :as str]
            [com.badlogic.gdx.utils.align :as align])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont)))

(defn text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn configure!
  [^BitmapFont font {:keys [scale
                            enable-markup?
                            use-integer-positions?]}]
  (.setScale (.getData font) scale)
  (set! (.markupEnabled (.getData font)) enable-markup?)
  (.setUseIntegerPositions font use-integer-positions?)
  font)
