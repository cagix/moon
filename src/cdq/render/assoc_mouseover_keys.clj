(ns cdq.render.assoc-mouseover-keys
  (:require [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.utils.viewport :as viewport]
            [cdq.math :as math]))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
; TODO clamping only works for gui-viewport ?
; TODO ? "Can be negative coordinates, undefined cells."
(defn- unproject-clamp [viewport [x y]]
  (viewport/unproject viewport
                      (math/clamp x
                                  (:viewport/left-gutter-width viewport)
                                  (:viewport/right-gutter-x    viewport))
                      (math/clamp y
                                  (:viewport/top-gutter-height viewport)
                                  (:viewport/top-gutter-y      viewport))))

(defn do!
  [{:keys [ctx/input
           ctx/stage
           ctx/ui-viewport
           ctx/world-viewport]
    :as ctx}]
  (let [mouse-position (input/mouse-position input)
        ui-mouse-position    (unproject-clamp ui-viewport mouse-position)
        world-mouse-position (unproject-clamp world-viewport mouse-position)]
    (assoc ctx
           :ctx/mouseover-actor      (stage/hit stage ui-mouse-position)
           :ctx/ui-mouse-position    ui-mouse-position
           :ctx/world-mouse-position world-mouse-position)))
