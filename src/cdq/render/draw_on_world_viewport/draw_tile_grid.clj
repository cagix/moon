(ns cdq.render.draw-on-world-viewport.draw-tile-grid
  (:require [clojure.ctx :as ctx]
            [clojure.gdx.graphics.camera :as camera]))

(def ^:dbg-flag show-tile-grid? false)

(defn- draw-tile-grid* [world-viewport]
  (let [[left-x _right-x bottom-y _top-y] (camera/frustum (:camera world-viewport))]
    [[:draw/grid
      (int left-x)
      (int bottom-y)
      (inc (int (:width world-viewport)))
      (+ 2 (int (:height world-viewport)))
      1
      1
      [1 1 1 0.8]]]))

(defn do! [{:keys [ctx/world-viewport] :as ctx}]
  (when show-tile-grid?
    (ctx/handle-draws! ctx (draw-tile-grid* world-viewport))))
