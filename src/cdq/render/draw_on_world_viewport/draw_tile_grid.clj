(ns cdq.render.draw-on-world-viewport.draw-tile-grid
  (:require [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera]))

(def ^:dbg-flag show-tile-grid? false)

(defn- draw-tile-grid* [world-viewport]
  (let [[left-x _right-x bottom-y _top-y] (camera/frustum (:viewport/camera world-viewport))]
    [[:draw/grid
      (int left-x)
      (int bottom-y)
      (inc (int (:viewport/width world-viewport)))
      (+ 2 (int (:viewport/height world-viewport)))
      1
      1
      [1 1 1 0.8]]]))

(defn do! [{:keys [ctx/graphics] :as ctx}]
  (when show-tile-grid?
    (graphics/handle-draws! graphics (draw-tile-grid* (:world-viewport graphics)))))
