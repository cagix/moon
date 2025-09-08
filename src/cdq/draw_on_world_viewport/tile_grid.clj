(ns cdq.draw-on-world-viewport.tile-grid
  (:require [cdq.graphics :as graphics]
            [clojure.gdx.graphics.camera :as camera]))

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

(def ^:dbg-flag show-tile-grid? false)

(defn do!
  [{:keys [ctx/world-viewport]
    :as ctx}]
  (when show-tile-grid?
    (graphics/handle-draws! ctx (draw-tile-grid* world-viewport))))
