(ns cdq.game.highlight-mouseover-tile
  (:require [cdq.ctx :as ctx]
            [cdq.viewport :as viewport]
            [cdq.graphics :as graphics]))

(defn do! []
  (let [[x y] (mapv int (viewport/mouse-position ctx/world-viewport))
        cell ((:grid ctx/world) [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      (graphics/draw-rectangle x y 1 1
                               (case (:movement @cell)
                                 :air  [1 1 0 0.5]
                                 :none [1 0 0 0.5])))))
