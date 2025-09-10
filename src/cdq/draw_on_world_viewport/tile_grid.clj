(ns cdq.draw-on-world-viewport.tile-grid
  (:require [cdq.gdx.graphics :as graphics]))

(def ^:dbg-flag show-tile-grid? false)

(defn do!
  [ctx]
  (when show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (graphics/camera-frustum ctx)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (graphics/world-viewport-width  ctx)))
        (+ 2 (int (graphics/world-viewport-height ctx)))
        1
        1
        [1 1 1 0.8]]])))
