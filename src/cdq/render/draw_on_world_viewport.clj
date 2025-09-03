(ns cdq.render.draw-on-world-viewport
  (:require [cdq.graphics :as graphics]
            [cdq.gdx.graphics.camera :as camera]
            [cdq.world.grid :as grid]
            [cdq.raycaster :as raycaster]
            [cdq.stacktrace :as stacktrace]
            [cdq.gdx.math.geom :as geom]
            [cdq.utils :as utils]))

(declare render-layers)

(def ^:dbg-flag show-tile-grid? false)
(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(defn- draw-tile-grid* [world-viewport]
  (let [[left-x _right-x bottom-y _top-y] (camera/frustum (:viewport/camera world-viewport))]
    [[:draw/grid
      (int left-x)
      (int bottom-y)
      (inc (int (:viewport/width world-viewport)))
      (+ 2 (int (:viewport/height world-viewport)))
      1
      1
      [1 1 1 0.8]]]))

(defn- draw-tile-grid [{:keys [ctx/graphics] :as ctx}]
  (when show-tile-grid?
    (graphics/handle-draws! graphics (draw-tile-grid* (:world-viewport graphics)))))

(defn- draw-cell-debug* [{:keys [ctx/world
                                 ctx/graphics]}]
  (let [grid (:world/grid world)]
    (apply concat
           (for [[x y] (camera/visible-tiles (:viewport/camera (:world-viewport graphics)))
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
                    (let [ratio (/ distance ((:world/factions-iterations world) faction))]
                      [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))]))))

(defn- draw-cell-debug [{:keys [ctx/graphics] :as ctx}]
  (graphics/handle-draws! graphics (draw-cell-debug* ctx)))

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-body-rect [{:keys [body/position body/width body/height]} color]
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    [[:draw/rectangle x y width height color]]))

(defn- draw-entity [{:keys [ctx/graphics] :as ctx} entity render-layer]
  (try
   (when show-body-bounds?
     (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) (if (:body/collides? (:entity/body entity)) :white :gray))))
   ; not doseq k v but doseq render-layer-components ...
   (doseq [[k v] entity
           :let [draw-fn (get render-layer k)]
           :when draw-fn]
     (graphics/handle-draws! graphics (draw-fn v entity ctx)))
   (catch Throwable t
     (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) :red))
     (stacktrace/pretty-print t))))

(defn- render-entities
  [{:keys [ctx/player-eid
           ctx/world]
    :as ctx}]
  (let [entities (map deref (:world/active-entities world))
        raycaster (:world/raycaster world)
        player @player-eid
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (raycaster/line-of-sight? raycaster player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))

(defn- geom-test*
  [{:keys [ctx/world
           ctx/world-mouse-position]}]
  (let [grid (:world/grid world)
        position world-mouse-position
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{:keys [x y width height]} (geom/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn- geom-test [{:keys [ctx/graphics] :as ctx}]
  (graphics/handle-draws! graphics (geom-test* ctx)))

(defn- highlight-mouseover-tile
  [{:keys [ctx/graphics
           ctx/world
           ctx/world-mouse-position]}]
  (graphics/handle-draws! graphics
                          (let [[x y] (mapv int world-mouse-position)
                                cell (grid/cell (:world/grid world) [x y])]
                            (when (and cell (#{:air :none} (:movement @cell)))
                              [[:draw/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5])]]))))


(defn do!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (doseq [f [draw-tile-grid
                                                 draw-cell-debug
                                                 render-entities
                                                 ; geom-test
                                                 highlight-mouseover-tile]]
                                        (f ctx))))
  ctx)
