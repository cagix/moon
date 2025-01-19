(ns cdq.render.after-entities
  (:require cdq.graphics
            [cdq.graphics.shape-drawer :as sd]
            [cdq.grid :as grid]
            [cdq.math.shapes :refer [circle->outer-rectangle]]))

(defn- geom-test [{:keys [cdq.graphics/shape-drawer
                          cdq.context/grid
                          cdq.graphics/world-viewport]}]
  (let [position (cdq.graphics/world-mouse-position world-viewport)
        radius 0.8
        circle {:position position :radius radius}]
    (sd/circle shape-drawer position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
      (sd/rectangle shape-drawer x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (sd/rectangle shape-drawer x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile [{:keys [cdq.graphics/shape-drawer
                                         cdq.context/grid
                                         cdq.graphics/world-viewport]}]
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (cdq.graphics/world-mouse-position world-viewport))
          cell (grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (sd/rectangle shape-drawer x y 1 1
                      (case (:movement @cell)
                        :air  [1 1 0 0.5]
                        :none [1 0 0 0.5]))))))

(defn render [c]
  #_(geom-test c)
  (highlight-mouseover-tile c))
