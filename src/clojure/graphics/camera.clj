(ns clojure.graphics.camera
  (:import (com.badlogic.gdx.graphics Camera OrthographicCamera)
           (com.badlogic.gdx.math Frustum Vector3)))

(defn zoom [^OrthographicCamera camera]
  (.zoom camera))

(defn position
  "Returns camera position as [x y] vector."
  [^Camera camera]
  [(.x (.position camera))
   (.y (.position camera))])

(defn combined
  "The combined projection and view matrix."
  [^Camera camera]
  (.combined camera))

(defn- vector3->clj-vec [^Vector3 v3]
  [(.x v3)
   (.y v3)
   (.z v3)])

(defn- frustum-plane-points [^Frustum frustum]
  (map vector3->clj-vec (.planePoints frustum)))

(defn frustum [^Camera camera]
  (let [frustum-points (take 4 (frustum-plane-points (.frustum camera)))
        left-x   (apply min (map first  frustum-points))
        right-x  (apply max (map first  frustum-points))
        bottom-y (apply min (map second frustum-points))
        top-y    (apply max (map second frustum-points))]
    [left-x right-x bottom-y top-y]))

(defn set-position!
  "Sets x and y and calls update on the camera."
  [^Camera camera [x y]]
  (set! (.x (.position camera)) (float x))
  (set! (.y (.position camera)) (float y))
  (.update camera))

(defn set-zoom!
  "Sets the zoom value and updates."
  [^OrthographicCamera camera amount]
  (set! (.zoom camera) amount)
  (.update camera))

(defn viewport-width [^Camera camera]
  (.viewportWidth camera))

(defn viewport-height [^Camera camera]
  (.viewportHeight camera))

(defn reset-zoom!
  "Sets the zoom value to 1."
  [camera]
  (set-zoom! camera 1))

(defn inc-zoom! [camera by]
  (set-zoom! camera (max 0.1 (+ (zoom camera) by))))
