(ns cdq.render.assoc-mouseover-keys
  (:require [clojure.gdx.scenes.scene2d.stage :as stage]
            [cdq.math :as math])
  (:import (com.badlogic.gdx Input)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils.viewport Viewport)))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
; TODO clamping only works for gui-viewport ?
; TODO ? "Can be negative coordinates, undefined cells."
(defn- unproject-clamp [^Viewport viewport [x y]]
  (let [x (math/clamp x
                      (.getLeftGutterWidth viewport)
                      (.getRightGutterX    viewport))
        y (math/clamp y
                      (.getTopGutterHeight viewport)
                      (.getTopGutterY      viewport))]
    (let [vector2 (.unproject viewport (Vector2. x y))]
      [(.x vector2)
       (.y vector2)])))

(defn do!
  [{:keys [ctx/input
           ctx/stage
           ctx/ui-viewport
           ctx/world-viewport]
    :as ctx}]
  (let [mouse-position [(Input/.getX input) (Input/.getY input)]
        ui-mouse-position    (unproject-clamp ui-viewport mouse-position)
        world-mouse-position (unproject-clamp world-viewport mouse-position)]
    (assoc ctx
           :ctx/mouseover-actor      (stage/hit stage ui-mouse-position)
           :ctx/ui-mouse-position    ui-mouse-position
           :ctx/world-mouse-position world-mouse-position)))
