(ns cdq.render.draw-on-world-viewport
  (:require [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.grid :as grid]
            [cdq.entity :as entity]
            [cdq.math :as math]
            [cdq.stacktrace :as stacktrace]
            [gdl.utils :as utils]))

(def ^:dbg-flag show-tile-grid? false)
(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)
(def ^:dbg-flag show-body-bounds? false)

(defn- draw-tile-grid* [graphics]
  (let [[left-x _right-x bottom-y _top-y] (graphics/camera-frustum graphics)]
    [[:draw/grid
      (int left-x)
      (int bottom-y)
      (inc (int (graphics/world-viewport-width graphics)))
      (+ 2 (int (graphics/world-viewport-height graphics)))
      1
      1
      [1 1 1 0.8]]]))

(defn- draw-tile-grid [{:keys [ctx/graphics]}]
  (graphics/handle-draws! graphics (draw-tile-grid* graphics)))

(defn- draw-cell-debug* [{:keys [ctx/graphics
                                 ctx/grid
                                 ctx/factions-iterations]}]
  (apply concat
         (for [[x y] (graphics/visible-tiles graphics)
               :let [cell (grid/cell grid [x y])]
               :when cell
               :let [cell* @cell]]
           [(when (and show-cell-entities? (seq (:entities cell*)))
              [:draw/filled-rectangle x y 1 1 [1 0 0 0.6]])
            (when (and show-cell-occupied? (seq (:occupied cell*)))
              [:draw/filled-rectangle x y 1 1 [0 0 1 0.6]])
            (when-let [faction show-potential-field-colors?]
              (let [{:keys [distance]} (faction cell*)]
                (when distance
                  (let [ratio (/ distance (factions-iterations faction))]
                    [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))])))

(defn- draw-cell-debug [{:keys [ctx/graphics]
                         :as ctx}]
  (graphics/handle-draws! graphics (draw-cell-debug* ctx)))

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    [[:draw/rectangle x y (:width entity) (:height entity) color]]))

(defn- render-entities! [{:keys [ctx/graphics
                                 ctx/active-entities
                                 ctx/player-eid
                                 ctx/render-z-order]
                          :as ctx}]
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (utils/sort-by-order (group-by :z-order entities)
                                                    first
                                                    render-z-order)
            render! [#'entity/render-below!
                     #'entity/render-default!
                     #'entity/render-above!
                     #'entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (g/line-of-sight? ctx player entity))]
      (try
       (when show-body-bounds?
         (graphics/handle-draws! graphics (draw-body-rect entity (if (:collides? entity) :white :gray))))
       (doseq [component entity]
         (graphics/handle-draws! graphics (render! component entity ctx)))
       (catch Throwable t
         (graphics/handle-draws! graphics (draw-body-rect entity :red))
         (stacktrace/pretty-pst t))))))

(defn- geom-test* [{:keys [ctx/grid] :as ctx}]
  (let [position (g/world-mouse-position ctx)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{[x y] :left-bottom
                 :keys [width height]} (math/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn- geom-test [{:keys [ctx/graphics]
                   :as ctx}]
  (graphics/handle-draws! graphics (geom-test* ctx)))

(defn- highlight-mouseover-tile* [{:keys [ctx/grid] :as ctx}]
  (let [[x y] (mapv int (g/world-mouse-position ctx))
        cell (grid/cell grid [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(defn- highlight-mouseover-tile [{:keys [ctx/graphics]
                                  :as ctx}]
  (graphics/handle-draws! graphics (highlight-mouseover-tile* ctx)))

(defn do! [{:keys [ctx/graphics]
            :as ctx}]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (doseq [f [(when show-tile-grid?
                                                   draw-tile-grid)
                                                 draw-cell-debug
                                                 render-entities!
                                                 ;geom-test
                                                 highlight-mouseover-tile]
                                              :when f]
                                        (f ctx))))
  ctx)
