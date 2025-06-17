(ns gdx.graphics.g2d.batch
  (:import (com.badlogic.gdx.graphics.g2d Batch)))

(def set-color!             Batch/.setColor)
(def set-projection-matrix! Batch/.setProjectionMatrix)
(def begin!                 Batch/.begin)
(def end!                   Batch/.end)

(defn draw! [^Batch batch texture-region [x y] [w h] rotation]
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
