(ns clojure.gdx.graphics.g2d.batch
  (:import (com.badlogic.gdx.graphics.g2d Batch)))

(defn draw! [^Batch batch texture-region x y [w h] rotation]
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; origin-x
         (/ (float h) 2) ; origin-y
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation))

(defn set-color! [^Batch batch [r g b a]]
  (.setColor batch r g b a))

(defn set-projection-matrix! [^Batch batch matrix]
  (.setProjectionMatrix batch matrix))

(defn begin! [^Batch batch]
  (.begin batch))

(defn end! [^Batch batch]
  (.end batch))
