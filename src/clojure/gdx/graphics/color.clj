(ns clojure.gdx.graphics.color
  (:import (com.badlogic.gdx.graphics Color)))

(def white Color/WHITE)
(def black Color/BLACK)

(defn create [r g b a]
  (Color. (float r)
          (float g)
          (float b)
          (float a)))
