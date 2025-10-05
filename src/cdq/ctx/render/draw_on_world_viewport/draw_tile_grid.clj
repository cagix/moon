(ns cdq.ctx.render.draw-on-world-viewport.draw-tile-grid
  (:require [cdq.graphics.camera :as camera]
            [cdq.graphics.world-viewport :as world-viewport]))

(def ^:dbg-flag show-tile-grid? false)

(defn do!
  [{:keys [ctx/graphics]}]
  (when show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (camera/frustum graphics)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (world-viewport/width  graphics)))
        (+ 2 (int (world-viewport/height graphics)))
        1
        1
        [1 1 1 0.8]]])))
