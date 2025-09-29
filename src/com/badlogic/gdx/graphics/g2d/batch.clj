(ns com.badlogic.gdx.graphics.g2d.batch
  (:import (com.badlogic.gdx.graphics.g2d Batch)))

(defn draw! [^Batch this texture-region x y [w h] rotation]
  (.draw this
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

(defn set-color! [^Batch this [r g b a]]
  (.setColor this r g b a))

(defn set-projection-matrix! [^Batch this matrix]
  (.setProjectionMatrix this matrix))

(defn begin! [^Batch this]
  (.begin this))

(defn end! [^Batch this]
  (.end this))
