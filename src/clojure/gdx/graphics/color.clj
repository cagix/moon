(ns clojure.gdx.graphics.color
  (:import (com.badlogic.gdx.graphics Color)))

(def black Color/BLACK)
(def white Color/WHITE)

(defn create
  ([r g b]
   (create r g b 1))
  ([r g b a]
   (Color. (float r) (float g) (float b) (float a))))
