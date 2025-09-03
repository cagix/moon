(ns cdq.draw-on-world-viewport.highlight-mouseover-tile
  (:require [cdq.graphics :as graphics]
            [cdq.world.grid :as grid]))

(defn do!
  [{:keys [ctx/graphics
           ctx/world
           ctx/world-mouse-position]}]
  (graphics/handle-draws! graphics
                          (let [[x y] (mapv int world-mouse-position)
                                cell (grid/cell (:world/grid world) [x y])]
                            (when (and cell (#{:air :none} (:movement @cell)))
                              [[:draw/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5])]]))))
