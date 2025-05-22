(ns cdq.render.draw-on-world-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.g :as g]
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

; TODO can still just call side effecty draws in entity/render-info ...

; cdq.draw still used: ui-actors ( ? ) , draw-gui-view

(defmulti draw! (fn [[k] _ctx]
                  k))

(defn handle-draws! [ctx draws]
  (doseq [component draws]
    (draw! component ctx)))

(defmethod draw! :draw/text [[_ opts] ctx]
  (draw/text ctx opts))

(defmethod draw! :draw/filled-rectangle [[_ & opts] ctx]
  (apply draw/filled-rectangle ctx opts))

(defmethod draw! :draw/rotated-centered [[_ & opts] ctx]
  (apply draw/rotated-centered ctx opts))

(defmethod draw! :draw/centered [[_ & opts] ctx]
  (apply draw/centered ctx opts))

(defmethod draw! :draw/image [[_ & opts] ctx]
  (apply draw/image ctx opts))

(defmethod draw! :draw/line [[_ & opts] ctx]
  (apply draw/line ctx opts))

(defmethod draw! :draw/circle [[_ & opts] ctx]
  (apply draw/circle ctx opts))

(defmethod draw! :draw/filled-circle [[_ & opts] ctx]
  (apply draw/filled-circle ctx opts))

(defmethod draw! :draw/ellipse [[_ & opts] ctx]
  (apply draw/ellipse ctx opts))

(defmethod draw! :draw/sector [[_ & opts] ctx]
  (apply draw/sector ctx opts))

(defmethod draw! :draw/with-line-width [[_ width draws] ctx]
  (draw/with-line-width ctx width
    (fn []
      (handle-draws! ctx draws))))

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
         (draw-body-rect ctx entity (if (:collides? entity) :white :gray)))
       (doseq [component entity]
         (handle-draws! ctx (render! component entity ctx)))
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
                                 (doseq [f draw-fns]
                                   (f (assoc ctx :ctx/unit-scale world-unit-scale)))))))
  nil)
