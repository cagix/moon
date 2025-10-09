(ns cdq.game.render.draw-on-world-view
  (:require [cdq.graphics :as graphics]
            [cdq.graphics.world-viewport :as world-viewport]
            [cdq.graphics.color :as color]
            [cdq.world.grid :as grid]
            [cdq.world.raycaster :as raycaster]
            [clojure.math.geom :as geom]
            [clojure.throwable :as throwable]
            [clojure.utils :as utils]))

(def ^:private render-layers
  (map
   #(update-vals % requiring-resolve)
   '[{:entity/mouseover?     cdq.entity.mouseover.draw/txs
      :stunned               cdq.entity.state.stunned.draw/txs
      :player-item-on-cursor cdq.entity.state.player-item-on-cursor.draw/txs}
     {:entity/clickable      cdq.entity.clickable.draw/txs
      :entity/animation      cdq.entity.animation.draw/txs
      :entity/image          cdq.entity.image.draw/txs
      :entity/line-render    cdq.entity.line-render.draw/txs}
     {:npc-sleeping          cdq.entity.state.npc-sleeping.draw/txs
      :entity/temp-modifier  cdq.entity.temp-modifier.draw/txs
      :entity/string-effect  cdq.entity.string-effect.draw/txs}
     {:entity/stats          cdq.entity.stats.draw/txs
      :active-skill          cdq.entity.state.active-skill.draw/txs}]))

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
          (graphics/draw! graphics (draw-body-rect (:entity/body entity)
                                                   (if (:body/collides? (:entity/body entity))
                                                     color/white
                                                     color/gray))))
        (doseq [[k v] entity
                :let [draw-fn (get render-layer k)]
                :when draw-fn]
          (graphics/draw! graphics (draw-fn v entity ctx))))
       (catch Throwable t
         (graphics/draw! graphics (draw-body-rect (:entity/body entity) color/red))
         (throwable/pretty-pst t))))

(defn draw-entities
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [entities (map deref (:world/active-entities world))
        player @(:world/player-eid world)
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (raycaster/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))

(def ^:dbg-flag show-tile-grid? false)

(defn draw-tile-grid
  [{:keys [ctx/graphics]}]
  (when show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (graphics/frustum graphics)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (world-viewport/width  graphics)))
        (+ 2 (int (world-viewport/height graphics)))
        1
        1
        [1 1 1 0.8]]])))

(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(defn draw-cell-debug
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

(defn geom-test
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
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn highlight-mouseover-tile
  [{:keys [ctx/graphics
           ctx/world]}]
  (let [[x y] (mapv int (:graphics/world-mouse-position graphics))
        cell ((:world/grid world) [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(defn step
  [{:keys [ctx/graphics]
    :as ctx} ]
  (world-viewport/draw! graphics
                        (fn []
                          (doseq [f [draw-tile-grid
                                     draw-cell-debug
                                     draw-entities
                                     #_geom-test
                                     highlight-mouseover-tile]]
                            (graphics/draw! graphics (f ctx)))))
  ctx)
