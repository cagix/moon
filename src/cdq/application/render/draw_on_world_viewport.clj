(ns cdq.application.render.draw-on-world-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.math :as math]
            [cdq.utils :refer [sort-by-order
                               pretty-pst]]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.viewport :as viewport]))

(defn- geom-test! [{:keys [ctx/world-viewport
                           ctx/grid]
                    :as ctx}]
  (let [position (viewport/mouse-position world-viewport)
        radius 0.8
        circle {:position position :radius radius}]
    (draw/circle ctx position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
      (draw/rectangle ctx x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (math/circle->outer-rectangle circle)]
      (draw/rectangle ctx x y width height [0 0 1 1]))))

(defn- highlight-mouseover-tile! [{:keys [ctx/world-viewport
                                          ctx/grid]
                                   :as ctx}]
  (let [[x y] (mapv int (viewport/mouse-position world-viewport))
        cell (grid [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      (draw/rectangle ctx x y 1 1
                      (case (:movement @cell)
                        :air  [1 1 0 0.5]
                        :none [1 0 0 0.5])))))

(defn- draw-body-rect [ctx entity color]
  (let [[x y] (:left-bottom entity)]
    (draw/rectangle ctx x y (:width entity) (:height entity) color)))

(defn- debug-draw-before-entities! [{:keys [ctx/world-viewport
                                            ctx/grid]
                                     :as ctx}]
  (let [cam (:camera world-viewport)
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when ctx/show-tile-grid?
      (draw/grid ctx
                 (int left-x) (int bottom-y)
                 (inc (int (:width  world-viewport)))
                 (+ 2 (int (:height world-viewport)))
                 1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell (grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and ctx/show-cell-entities? (seq (:entities cell*)))
        (draw/filled-rectangle ctx x y 1 1 [1 0 0 0.6]))

      (when (and ctx/show-cell-occupied? (seq (:occupied cell*)))
        (draw/filled-rectangle ctx x y 1 1 [0 0 1 0.6]))

      (when-let [faction ctx/show-potential-field-colors?]
        (let [{:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (ctx/factions-iterations faction))]
              (draw/filled-rectangle ctx x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- render-entities! [{:keys [ctx/active-entities
                                 ctx/player-eid]
                          :as ctx}]
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              ctx/render-z-order)
            render! [entity/render-below!
                     entity/render-default!
                     entity/render-above!
                     entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (entity/line-of-sight? ctx player entity))]
      (try
       (when ctx/show-body-bounds?
         (draw-body-rect ctx entity (if (:collides? entity) :white :gray)))
       (doseq [component entity]
         (render! component entity ctx))
       (catch Throwable t
         (draw-body-rect ctx entity :red)
         (pretty-pst t))))))

(def draw-fns
  [debug-draw-before-entities!
   render-entities!
   ; geom-test!
   highlight-mouseover-tile!])

(defn do! [{:keys [ctx/batch
                   ctx/world-viewport
                   ctx/world-unit-scale
                   ctx/unit-scale]
            :as ctx}]
  (batch/draw-on-viewport! batch
                           world-viewport
                           (fn []
                             (draw/with-line-width ctx world-unit-scale
                               (fn []
                                 (reset! unit-scale world-unit-scale)
                                 (doseq [f draw-fns]
                                   (f ctx))
                                 (reset! unit-scale 1))))))
