(ns cdq.render.draw-on-world-viewport.highlight-mouseover-tile
  (:require [cdq.grid :as grid]
            [gdl.c :as c]
            [gdl.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics
                   ctx/grid] :as ctx}]
  (graphics/handle-draws! graphics
                          (let [[x y] (mapv int (c/world-mouse-position ctx))
                                cell (grid/cell grid [x y])]
                            (when (and cell (#{:air :none} (:movement @cell)))
                              [[:draw/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5])]]))))
