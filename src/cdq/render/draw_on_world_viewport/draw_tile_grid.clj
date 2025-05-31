(ns cdq.render.draw-on-world-viewport.draw-tile-grid
  (:require [cdq.g :as g]))

(def ^:dbg-flag show-tile-grid? false)

(defn- draw-tile-grid* [ctx]
  (let [[left-x _right-x bottom-y _top-y] (g/camera-frustum ctx)]
    [[:draw/grid
      (int left-x)
      (int bottom-y)
      (inc (int (g/world-viewport-width ctx)))
      (+ 2 (int (g/world-viewport-height ctx)))
      1
      1
      [1 1 1 0.8]]]))

(defn do! [ctx]
  (when show-tile-grid?
    (g/handle-draws! ctx (draw-tile-grid* ctx))))
