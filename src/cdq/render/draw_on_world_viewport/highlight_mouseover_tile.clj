(ns cdq.render.draw-on-world-viewport.highlight-mouseover-tile)

(defn do!
  [{:keys [ctx/world
           ctx/world-mouse-position]}]
  (let [[x y] (mapv int world-mouse-position)
        cell ((:world/grid world) [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))
