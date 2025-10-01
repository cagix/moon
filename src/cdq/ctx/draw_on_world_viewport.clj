(ns cdq.ctx.draw-on-world-viewport
  (:require [cdq.ctx.draw-entities :as draw-entities]
            [cdq.graphics :as graphics]
            [cdq.graphics.draws :as draws]
            [cdq.graphics.draw-on-world-viewport :as draw-on-world-viewport]
            [cdq.world.grid :as grid]))

(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)
(def ^:dbg-flag show-tile-grid? false)

(defn- draw-tile-grid
  [{:keys [ctx/graphics]}]
  (when show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (graphics/camera-frustum graphics)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (graphics/world-viewport-width  graphics)))
        (+ 2 (int (graphics/world-viewport-height graphics)))
        1
        1
        [1 1 1 0.8]]])))

(defn- draw-cell-debug
  [{:keys [ctx/graphics
           ctx/world]}]
  (apply concat
         (for [[x y] (graphics/visible-tiles graphics)
               :let [cell ((:world/grid world) [x y])]
               :when cell
               :let [cell* @cell]]
           [(when (and show-cell-entities? (seq (:entities cell*)))
              [:draw/filled-rectangle x y 1 1 [1 0 0 0.6]])
            (when (and show-cell-occupied? (seq (:occupied cell*)))
              [:draw/filled-rectangle x y 1 1 [0 0 1 0.6]])
            (when-let [faction show-potential-field-colors?]
              (let [{:keys [distance]} (faction cell*)]
                (when distance
                  (let [ratio (/ distance ((:world/factions-iterations world) faction))]
                    [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))])))

(defn- highlight-mouseover-tile
  [{:keys [ctx/graphics
           ctx/world]}]
  (let [[x y] (mapv int (:graphics/world-mouse-position graphics))
        cell ((:world/grid world) [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(comment
 (require '[gdl.math.geom :as geom]
          '[cdq.world.grid :as grid])

 (defn- geom-test
  [{:keys [ctx/graphics
           ctx/world]}]
  (let [position (:graphics/world-mouse-position graphics)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells (:world/grid world) circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{:keys [x y width height]} (geom/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]])))))

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}]
  (draw-on-world-viewport/do! graphics
                              (fn []
                                (doseq [f [draw-tile-grid
                                           draw-cell-debug
                                           draw-entities/do!
                                           #_geom-test
                                           highlight-mouseover-tile]]
                                  (draws/handle! graphics (f ctx)))))
  ctx)
