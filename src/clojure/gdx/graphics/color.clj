(ns clojure.gdx.graphics.color
  (:import (com.badlogic.gdx.graphics Color)))

(def white Color/WHITE)

(defn create
  ([r g b a]
   (Color. r g b a))
  ([[r g b a]]
   (Color. r g b a)))
