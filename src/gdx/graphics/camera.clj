(ns gdx.graphics.camera
  (:require [gdx.math.vector3 :as vector3])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(defn zoom [^OrthographicCamera this]
  (.zoom this))

(defn position [^OrthographicCamera this]
  [(.x (.position this))
   (.y (.position this))])

(defn frustum [^OrthographicCamera this]
  (let [frustum-points (take 4 (map vector3/clojurize (.planePoints (.frustum this))))
        left-x   (apply min (map first  frustum-points))
        right-x  (apply max (map first  frustum-points))
        bottom-y (apply min (map second frustum-points))
        top-y    (apply max (map second frustum-points))]
    [left-x right-x bottom-y top-y]))

(defn set-position! [^OrthographicCamera this [x y]]
  (set! (.x (.position this)) (float x))
  (set! (.y (.position this)) (float y))
  (.update this))

(defn set-zoom! [^OrthographicCamera this amount]
  (set! (.zoom this) amount)
  (.update this))

(defn viewport-width [^OrthographicCamera this]
  (.viewportWidth this))

(defn viewport-height [^OrthographicCamera this]
  (.viewportHeight this))

(defn reset-zoom! [cam]
  (set-zoom! cam 1))

(defn inc-zoom! [cam by]
  (set-zoom! cam (max 0.1 (+ (zoom cam) by))))
