(ns clojure.gdx.utils.viewport
  (:refer-clojure :exclude [update])
  (:require [clojure.gdx.input :as input]
            [clojure.gdx.math.utils :refer [clamp]])
  (:import (com.badlogic.gdx.utils.viewport FitViewport Viewport)
           (com.badlogic.gdx.math Vector2)))

(defn fit-viewport [width height camera]
  (FitViewport. width height camera))

(defn update [^Viewport viewport w h & {:keys [center-camera?]}]
  (.update viewport w h (boolean center-camera?)))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
(defn unproject-mouse-position
  "Returns vector of [x y]."
  [^Viewport viewport]
  (let [mouse-x (clamp (input/x)
                       (.getLeftGutterWidth viewport)
                       (.getRightGutterX viewport))
        mouse-y (clamp (input/y)
                       (.getTopGutterHeight viewport)
                       (.getTopGutterY viewport))
        coords (.unproject viewport (Vector2. mouse-x mouse-y))]
    [(.x coords) (.y coords)]))
