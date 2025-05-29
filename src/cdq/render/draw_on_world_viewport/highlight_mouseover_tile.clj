(ns cdq.render.draw-on-world-viewport.highlight-mouseover-tile
  (:require [cdq.g :as g]
            [cdq.grid :as grid]
            [gdl.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics
                   ctx/grid]
            :as ctx}]
  (graphics/handle-draws! graphics
                          (let [[x y] (mapv int (g/world-mouse-position ctx))
                                cell (grid/cell grid [x y])]
                            (when (and cell (#{:air :none} (:movement @cell)))
                              [[:draw/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5])]]))))
