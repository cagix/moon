(ns clojure.gdx.graphics.color
  (:import (com.badlogic.gdx.graphics Color)))

(def black [0 0 0 1])
(def white [1 1 1 1])
(def gray  [0.5 0.5 0.5 1])
(def red   [1 0 0 1])

(defn ->obj ^Color [input]
  (assert (vector? input)
          (pr-str input))
  (let [[r g b a] input]
    (Color. r g b a)))

(defn float-bits [[r g b a]]
  (Color/toFloatBits (float r)
                     (float g)
                     (float b)
                     (float a)))
