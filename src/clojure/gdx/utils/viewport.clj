(ns clojure.gdx.utils.viewport
  (:refer-clojure :exclude [update])
  (:import (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils.viewport FitViewport Viewport)))

(defn fit [viewport-width viewport-height camera]
  (FitViewport. viewport-width viewport-height camera))

(defn update [viewport w h & {:keys [center-camera?]}]
  (Viewport/.update viewport w h (boolean center-camera?)))

(def camera Viewport/.getCamera)

(defn unproject [viewport x y]
  (let [v2 (Viewport/.unproject viewport (Vector2. x y))]
    [(.x v2) (.y v2)]))

(def left-gutter-width Viewport/.getLeftGutterWidth)
(def right-gutter-x    Viewport/.getRightGutterX)
(def top-gutter-height Viewport/.getTopGutterHeight)
(def top-gutter-y      Viewport/.getTopGutterY)
