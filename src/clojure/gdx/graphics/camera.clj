(ns clojure.gdx.graphics.camera
  (:import (com.badlogic.gdx.graphics Camera)))

; also has z?
(defn position [^Camera this]
  [(.x (.position this))
   (.y (.position this))])

(defn set-position! [^Camera this [x y]]
  (set! (.x (.position this)) (float x))
  (set! (.y (.position this)) (float y))
  (.update this))
