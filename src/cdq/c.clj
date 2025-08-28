(ns cdq.c
  (:require [cdq.input :as input]
            [cdq.ui.stage :as stage])
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
(defn- unproject-clamp [^Viewport viewport [x y]]
  (let [x (clamp x
                 (.getLeftGutterWidth viewport)
                 (.getRightGutterX    viewport))
        y (clamp y
                 (.getTopGutterHeight viewport)
                 (.getTopGutterY      viewport))]
    (let [vector2 (.unproject viewport (Vector2. x y))]
      [(.x vector2)
       (.y vector2)])))

(defn world-mouse-position [{:keys [ctx/input
                                    ctx/graphics]}]
  (unproject-clamp (:world-viewport graphics) (input/mouse-position input)))

(defn ui-mouse-position [{:keys [ctx/input
                                 ctx/graphics]}]
  (unproject-clamp (:ui-viewport graphics) (input/mouse-position input)))

(defn mouseover-actor [{:keys [ctx/stage] :as ctx}]
  (stage/hit stage (ui-mouse-position ctx)))
