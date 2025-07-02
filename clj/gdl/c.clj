(ns gdl.c
  (:require [clojure.graphics.viewport :as viewport]
            [clojure.input :as input]
            [gdl.ui.stage :as stage])
  (:import (com.badlogic.gdx.utils.viewport Viewport)))

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
    (viewport/unproject viewport x y)))

(defn world-mouse-position [{:keys [ctx/input
                                    ctx/graphics]}]
  (unproject-clamp (:world-viewport graphics) (input/mouse-position input)))

(defn ui-mouse-position [{:keys [ctx/input
                                 ctx/graphics]}]
  (unproject-clamp (:ui-viewport graphics) (input/mouse-position input)))

(defn mouseover-actor [{:keys [ctx/stage] :as ctx}]
  (stage/hit stage (ui-mouse-position ctx)))
