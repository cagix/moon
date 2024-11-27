(ns clojure.gdx.graphics.color
  "A color class, holding the r, g, b and alpha component as floats in the range [0,1]. All methods perform clamping on the internal values after execution."
  (:import (com.badlogic.gdx.graphics Color)))

(def black Color/BLACK)
(def white Color/WHITE)

(defn create
  "Constructor, sets the components of the color

  Parameters:

  r - the red component

  g - the green component

  b - the blue component

  a - the alpha component "
  ([r g b]
   (create r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))
