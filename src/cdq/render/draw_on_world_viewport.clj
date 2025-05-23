(ns cdq.render.draw-on-world-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.grid :as grid]
            [cdq.math :as math]
            [cdq.utils :refer [sort-by-order
                               pretty-pst]]))

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

(defn- geom-test [ctx]
  (g/handle-draws! ctx (geom-test* ctx)))

(defn- highlight-mouseover-tile* [{:keys [ctx/grid] :as ctx}]
  (let [[x y] (mapv int (g/world-mouse-position ctx))
        cell (grid [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(defn- highlight-mouseover-tile [ctx]
  (g/handle-draws! ctx (highlight-mouseover-tile* ctx)))

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    [[:draw/rectangle x y (:width entity) (:height entity) color]]))

(defn- draw-tile-grid* [ctx]
  (when ctx/show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (g/camera-frustum ctx)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (g/world-viewport-width ctx)))
        (+ 2 (int (g/world-viewport-height ctx)))
        1
        1
        [1 1 1 0.8]]])))

(defn- draw-tile-grid [ctx]
  (g/handle-draws! ctx (draw-tile-grid* ctx)))

(defn- draw-cell-debug* [{:keys [ctx/grid] :as ctx}]
  (apply concat
         (for [[x y] (g/visible-tiles ctx)
               :let [cell (grid [x y])]
               :when cell
               :let [cell* @cell]]
           [(when (and ctx/show-cell-entities? (seq (:entities cell*)))
              [:draw/filled-rectangle x y 1 1 [1 0 0 0.6]])
            (when (and ctx/show-cell-occupied? (seq (:occupied cell*)))
              [:draw/filled-rectangle x y 1 1 [0 0 1 0.6]])
            (when-let [faction ctx/show-potential-field-colors?]
              (let [{:keys [distance]} (faction cell*)]
                (when distance
                  (let [ratio (/ distance (ctx/factions-iterations faction))]
                    [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))])))

(defn- draw-cell-debug [ctx]
  (g/handle-draws! ctx (draw-cell-debug* ctx)))

(defn- render-entities! [{:keys [ctx/active-entities
                                 ctx/player-eid]
                          :as ctx}]
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              ctx/render-z-order)
            render! [#'entity/render-below!
                     #'entity/render-default!
                     #'entity/render-above!
                     #'entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (g/line-of-sight? ctx player entity))]
      (try
       (when ctx/show-body-bounds?
         (g/handle-draws! ctx (draw-body-rect entity (if (:collides? entity) :white :gray))))
       (doseq [component entity]
         (g/handle-draws! ctx (render! component entity ctx)))
       (catch Throwable t
         (g/handle-draws! ctx (draw-body-rect entity :red))
         (pretty-pst t))))))

(defn do! [ctx]
  (g/draw-on-world-viewport! ctx [draw-tile-grid
                                  draw-cell-debug
                                  render-entities!
                                  ;geom-test
                                  highlight-mouseover-tile])
  nil)
