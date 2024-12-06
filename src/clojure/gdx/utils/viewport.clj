(ns clojure.gdx.utils.viewport
  (:refer-clojure :exclude [update])
  (:import (com.badlogic.gdx.utils.viewport FitViewport Viewport)))

(defn fit-viewport [width height camera]
  (FitViewport. width height camera))

(defn update [^Viewport viewport w h & {:keys [center-camera?]}]
  (.update viewport w h (boolean center-camera?)))
