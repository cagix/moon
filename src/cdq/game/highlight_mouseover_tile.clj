(ns cdq.game.highlight-mouseover-tile
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [gdl.graphics.viewport :as viewport]))

(defn do! []
  (let [[x y] (mapv int (viewport/mouse-position ctx/world-viewport))
        cell (ctx/grid [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      (draw/rectangle x y 1 1
                      (case (:movement @cell)
                        :air  [1 1 0 0.5]
                        :none [1 0 0 0.5])))))
