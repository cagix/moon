(ns cdq.render.draw-on-world-viewport.highlight-mouseover-tile
  (:require [cdq.grid :as grid]
            [cdq.c :as c]
            [cdq.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics
                   ctx/world] :as ctx}]
  (graphics/handle-draws! graphics
                          (let [[x y] (mapv int (c/world-mouse-position ctx))
                                cell (grid/cell (:world/grid world) [x y])]
                            (when (and cell (#{:air :none} (:movement @cell)))
                              [[:draw/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5])]]))))
