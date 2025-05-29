(ns cdq.render.draw-on-world-viewport.draw-tile-grid
  (:require [cdq.graphics :as graphics]))

(def ^:dbg-flag show-tile-grid? false)

(defn- draw-tile-grid* [graphics]
  (let [[left-x _right-x bottom-y _top-y] (graphics/camera-frustum graphics)]
    [[:draw/grid
      (int left-x)
      (int bottom-y)
      (inc (int (graphics/world-viewport-width graphics)))
      (+ 2 (int (graphics/world-viewport-height graphics)))
      1
      1
      [1 1 1 0.8]]]))

(defn do! [{:keys [ctx/graphics]}]
  (when show-tile-grid?
    (graphics/handle-draws! graphics (draw-tile-grid* graphics))))
