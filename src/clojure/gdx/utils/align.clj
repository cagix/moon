(ns clojure.gdx.utils.align
  (:import (com.badlogic.gdx.utils Align)))

(def mapping {:bottom      Align/bottom
              :bottomleft  Align/bottomLeft
              :bottomright Align/bottomRight
              :center      Align/center
              :left        Align/left
              :right       Align/right
              :top         Align/top
              :topleft     Align/topLeft
              :topright    Align/topRight})

(defn ->from-k [k]
  (when-not (contains? mapping k)
    (throw (IllegalArgumentException. (str "Unknown Align: " k ". \nOptions are:\n" (sort (keys mapping))))))
  (k mapping))
