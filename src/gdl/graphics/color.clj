(ns gdl.graphics.color
  (:import (com.badlogic.gdx.graphics Color)))

(defn create [r g b a]
  (Color. (float r)
          (float g)
          (float b)
          (float a)))

(def white Color/WHITE)
(def black Color/BLACK)
