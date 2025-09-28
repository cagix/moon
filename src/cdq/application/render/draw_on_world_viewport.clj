(ns cdq.application.render.draw-on-world-viewport
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.world :as world]
            [gdl.graphics.color :as color]
            [gdl.utils :as utils]))

(def ^:dbg-flag show-tile-grid? false)
(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(def ^:private render-layers
  '[{:entity/mouseover?     cdq.entity.mouseover/draw
     :stunned               cdq.entity.state.stunned/draw
     :player-item-on-cursor cdq.entity.state.player-item-on-cursor/draw}
    {:entity/clickable      cdq.entity.clickable/draw
     :entity/animation      cdq.entity.animation/draw
     :entity/image          cdq.entity.image/draw
     :entity/line-render    cdq.entity.line/draw}
    {:npc-sleeping          cdq.entity.state.npc-sleeping/draw
     :entity/temp-modifier  cdq.entity.temp-modifier/draw
     :entity/string-effect  cdq.entity.string-effect/draw}
    {:entity/stats        cdq.entity.stats/draw
     :active-skill          cdq.entity.state.active-skill/draw}])

(alter-var-root #'render-layers
                (fn [k->fns]
                  (map (fn [k->fn]
                         (update-vals k->fn
                                      (fn [sym]
                                        (let [avar (requiring-resolve sym)]
                                          (assert avar sym)
                                          avar))))
                       k->fns)))

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-body-rect [{:keys [body/position body/width body/height]} color]
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    [[:draw/rectangle x y width height color]]))

(defn- draw-entity
  [{:keys [ctx/graphics]
    :as ctx}
   entity render-layer]
  (try (do
        (when show-body-bounds?
          (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity)
                                                           (if (:body/collides? (:entity/body entity))
                                                             color/white
                                                             color/gray))))
        (doseq [[k v] entity
                :let [draw-fn (get render-layer k)]
                :when draw-fn]
          (graphics/handle-draws! graphics (draw-fn v entity ctx))))
       (catch Throwable t
         (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) color/red))
         (ctx/handle-txs! ctx
                          [[:tx/print-stacktrace t]]))))

(defn- draw-entities
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [entities (map deref (world/active-eids world))
        player @(:world/player-eid world)
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (world/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))

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

(defn- do!* [{:keys [ctx/graphics]
              :as ctx}]
  (doseq [f [
             draw-tile-grid
             draw-cell-debug
             draw-entities
             #_geom-test
             highlight-mouseover-tile
             ]]
    (graphics/handle-draws! graphics (f ctx))))

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (do!* ctx)))
  ctx)
