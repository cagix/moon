(ns cdq.render.draw-on-world-viewport.highlight-mouseover-tile
  (:require [cdq.grid :as grid]
            [cdq.ctx :as ctx]))

(defn do! [{:keys [ctx/grid] :as ctx}]
  (ctx/handle-draws! ctx
                     (let [[x y] (mapv int (ctx/world-mouse-position ctx))
                           cell (grid/cell grid [x y])]
                       (when (and cell (#{:air :none} (:movement @cell)))
                         [[:draw/rectangle x y 1 1
                           (case (:movement @cell)
                             :air  [1 1 0 0.5]
                             :none [1 0 0 0.5])]]))))
