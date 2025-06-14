(ns gdx.utils.viewport
  (:import (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils.viewport Viewport)))

(defn- clamp [value min max]
  (cond
   (< value min) min
   (> value max) max
   :else value))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
; TODO clamping only works for gui-viewport ?
; TODO ? "Can be negative coordinates, undefined cells."
(defn unproject [^Viewport viewport [x y]]
  (let [x (clamp x (.getLeftGutterWidth viewport) (.getRightGutterX    viewport))
        y (clamp y (.getTopGutterHeight viewport) (.getTopGutterY      viewport))]
    (let [vector2 (.unproject viewport (Vector2. x y))]
      [(.x vector2)
       (.y vector2)])))

(defn update! [^Viewport viewport width height center-camera?]
  (.update viewport width height center-camera?))
