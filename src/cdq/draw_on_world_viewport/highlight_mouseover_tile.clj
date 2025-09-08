(ns cdq.draw-on-world-viewport.highlight-mouseover-tile
  (:require [cdq.graphics :as graphics]
            [cdq.grid :as grid]))

(defn do!
  [{:keys [ctx/grid
           ctx/world-mouse-position]
    :as ctx}]
  (graphics/handle-draws! ctx
                     (let [[x y] (mapv int world-mouse-position)
                           cell (grid/cell grid [x y])]
                       (when (and cell (#{:air :none} (:movement @cell)))
                         [[:draw/rectangle x y 1 1
                           (case (:movement @cell)
                             :air  [1 1 0 0.5]
                             :none [1 0 0 0.5])]]))))
