(ns clojure.gdx.utils
  (:import (com.badlogic.gdx.utils Align
                                   Disposable
                                   Scaling
                                   ScreenUtils)))

(defn align [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

(defn scaling [k]
  (case k
    :fill Scaling/fill))

(defn clear-screen [color]
  (ScreenUtils/clear color))

(def dispose Disposable/.dispose)
