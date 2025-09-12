(ns clojure.gdx.graphics.color
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.utils NumberUtils)))

(defn create ^Color [[r g b a]]
  (Color. r g b a))

(defn float-bits [[r g b a]]
  (let [color (bit-or (bit-shift-left (int (* 255 (float a))) 24)
                      (bit-shift-left (int (* 255 (float b))) 16)
                      (bit-shift-left (int (* 255 (float g))) 8)
                      (int (* 255 (float r))))]
    (NumberUtils/intToFloatColor (unchecked-int color))))
