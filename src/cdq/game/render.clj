(ns cdq.game.render
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.graphics.textures :as textures]
            [cdq.graphics.tiled-map-renderer :as tiled-map-renderer]
            [cdq.graphics.ui-viewport :as ui-viewport]
            [cdq.graphics.world-viewport :as world-viewport]
            [cdq.graphics.color :as color]
            [cdq.input :as input]
            [cdq.ui :as ui]
            [cdq.entity.body :as body]
            [cdq.entity.state :as state]
            [cdq.entity.stats :as stats]
            [cdq.entity.skills.skill :as skill]
            [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [cdq.world.raycaster :as raycaster]
            [clojure.math.geom :as geom]
            [clojure.math.vector2 :as v]
            [clojure.txs :as txs]
            [clojure.throwable :as throwable]
            [clojure.utils :as utils]
            [malli.core :as m]
            [malli.utils :as mu])
  (:import (cdq.ui Stage)))

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

(defn render-stage
  [{:keys [^Stage ctx/stage]
    :as ctx}]
  (set! (.ctx stage) ctx)
  (.act  stage)
  (.draw stage)
  (.ctx  stage))

(def zoom-speed 0.025)

(defn window-camera-controls
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/zoom-in?            input) (graphics/change-zoom! graphics zoom-speed))
  (when (input/zoom-out?           input) (graphics/change-zoom! graphics (- zoom-speed)))
  (when (input/close-windows?      input) (ui/close-all-windows!         stage))
  (when (input/toggle-inventory?   input) (ui/toggle-inventory-visible!  stage))
  (when (input/toggle-entity-info? input) (ui/toggle-entity-info-window! stage))
  ctx)

(def destroy-components
  {:entity/destroy-audiovisual
   {:destroy! (fn [audiovisuals-id eid _ctx]
                [[:tx/audiovisual
                  (:body/position (:entity/body @eid))
                  audiovisuals-id]])}})

(defn remove-destroyed-entities
  [{:keys [ctx/world]
    :as ctx}]
  (let [{:keys [world/content-grid
                world/entity-ids
                world/grid]} world]
    (doseq [eid (filter (comp :entity/destroyed? deref)
                        (vals @entity-ids))]
      (let [id (:entity/id @eid)]
        (assert (contains? @entity-ids id))
        (swap! entity-ids dissoc id))
      (content-grid/remove-entity! content-grid eid)
      (grid/remove-from-touched-cells! grid eid)
      (when (:body/collides? (:entity/body @eid))
        (grid/remove-from-occupied-cells! grid eid))
      (txs/handle! ctx
                   (mapcat (fn [[k v]]
                             (when-let [destroy! (:destroy! (k destroy-components))]
                               (destroy! v eid ctx)))
                           @eid))))
  ctx)

(defn tick-entities
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do (try
         (txs/handle! ctx (world/tick-entities! world))
         (catch Throwable t
           (throwable/pretty-pst t)
           (ui/show-error-window! stage t)))
        ctx)))

(defn- player-effect-ctx [mouseover-eid world-mouse-position player-eid]
  (let [target-position (or (and mouseover-eid
                                 (:body/position (:entity/body @mouseover-eid)))
                            world-mouse-position)]
    {:effect/source player-eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:body/position (:entity/body @player-eid))
                                           target-position)}))

(defn- interaction-state
  [stage
   world-mouse-position
   mouseover-eid
   player-eid
   mouseover-actor]
  (cond
   mouseover-actor
   [:interaction-state/mouseover-actor (ui/actor-information stage mouseover-actor)]

   (and mouseover-eid
        (:entity/clickable @mouseover-eid))
   [:interaction-state/clickable-mouseover-eid
    {:clicked-eid mouseover-eid
     :in-click-range? (< (body/distance (:entity/body @player-eid)
                                        (:entity/body @mouseover-eid))
                         (:entity/click-distance-tiles @player-eid))}]

   :else
   (if-let [skill-id (ui/action-bar-selected-skill stage)]
     (let [entity @player-eid
           skill (skill-id (:entity/skills entity))
           effect-ctx (player-effect-ctx mouseover-eid world-mouse-position player-eid)
           state (skill/usable-state skill entity effect-ctx)]
       (if (= state :usable)
         [:interaction-state.skill/usable [skill effect-ctx]]
         [:interaction-state.skill/not-usable state]))
     [:interaction-state/no-skill-selected])))

(defn assoc-interaction-state
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (assoc ctx :ctx/interaction-state (interaction-state stage
                                                       (:graphics/world-mouse-position graphics)
                                                       (:world/mouseover-eid world)
                                                       (:world/player-eid    world)
                                                       (ui/mouseover-actor stage (input/mouse-position input)))))

(def ^:private schema
  (m/schema
   [:map {:closed true}
    [:ctx/audio :some]
    [:ctx/db :some]
    [:ctx/graphics :some]
    [:ctx/input :some]
    [:ctx/stage :some]
    [:ctx/actor-fns :some]
    [:ctx/world :some]]))

(defn- validate [ctx]
  (mu/validate-humanize schema ctx)
  ctx)

(defn- get-stage-ctx
  [{:keys [ctx/stage]
    :as ctx}]
  (or (ui/get-ctx stage)
      ctx)) ; first render stage does not have ctx set.

(defn- update-mouse
  [{:keys [ctx/graphics
           ctx/input]
    :as ctx}]
  (let [mp (input/mouse-position input)]
    (-> ctx
        (assoc-in [:ctx/graphics :graphics/world-mouse-position] (world-viewport/unproject graphics mp))
        (assoc-in [:ctx/graphics :graphics/ui-mouse-position   ] (ui-viewport/unproject    graphics mp))
        )))

(defn- get-mouseover-entity
  [{:keys [world/grid
           world/mouseover-eid
           world/player-eid
           world/render-z-order]
    :as world}
   position]
  (let [player @player-eid
        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                     (grid/point->entities grid position))]
    (->> render-z-order
         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
         reverse
         (filter #(raycaster/line-of-sight? world player @%))
         first)))

(defn- update-mouseover-eid
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (let [mouseover-actor (ui/mouseover-actor stage (input/mouse-position input))
        mouseover-eid (:world/mouseover-eid world)
        new-eid (if mouseover-actor
                  nil
                  (get-mouseover-entity world (:graphics/world-mouse-position graphics)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc-in ctx [:ctx/world :world/mouseover-eid] new-eid)))

(defn- check-open-debug
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/open-debug-button-pressed? input)
    (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                   @((:world/grid world) (mapv int (:graphics/world-mouse-position graphics))))]
      (ui/show-data-viewer! stage data)))
  ctx)

(defn- assoc-active-entities
  [{:keys [ctx/world]
    :as ctx}]
  (update ctx :ctx/world world/cache-active-entities))

(defn- set-camera-on-player!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/set-position! graphics
                          (:body/position
                           (:entity/body
                            @(:world/player-eid world))))
  ctx)

(defn- clear-screen!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear! graphics color/black)
  ctx)

(defn- tile-color-setter
  [{:keys [ray-blocked?
           explored-tile-corners
           light-position
           see-all-tiles?
           explored-tile-color
           visible-tile-color
           invisible-tile-color]}]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored?
                         explored-tile-color
                         invisible-tile-color)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (ray-blocked? light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles?
            visible-tile-color
            base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              visible-tile-color))))))

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn draw-world-map!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (tiled-map-renderer/draw! graphics
                            (:world/tiled-map world)
                            (tile-color-setter
                             {:ray-blocked? (partial raycaster/blocked? world)
                              :explored-tile-corners (:world/explored-tile-corners world)
                              :light-position (graphics/position graphics)
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]}))
  ctx)

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

(defn- draw-on-world-viewport!
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

(defn set-cursor!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        cursor-key (state/cursor [state-k (state-k entity)] eid ctx)]
    (graphics/set-cursor! graphics cursor-key))
  ctx)

(defn player-state-handle-input
  [{:keys [ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        txs (state/handle-input [state-k (state-k entity)] eid ctx)]
    (txs/handle! ctx txs))
  ctx)

(defn dissoc-interaction-state [ctx]
  (dissoc ctx :ctx/interaction-state))

(def pausing? true)

(def state->pause-game? {:stunned false
                         :player-moving false
                         :player-item-on-cursor true
                         :player-idle true
                         :player-dead true
                         :active-skill false})

(defn assoc-paused
  [{:keys [ctx/input
           ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (or #_error
                (and pausing?
                     (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                     (not (input/unpause? input))))))

(defn- update-world-time* [{:keys [world/max-delta]
                           :as world}
                          delta-ms]
  (let [delta-ms (min delta-ms max-delta)]
    (-> world
        (assoc :world/delta-time delta-ms)
        (update :world/elapsed-time + delta-ms))))

(defn update-world-time
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (update ctx :ctx/world update-world-time* (graphics/delta-time graphics))))

(defn update-potential-fields
  [{:keys [ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do
     (world/update-potential-fields! world)
     ctx)))

(defn do! [ctx]
  (-> ctx
      get-stage-ctx
      validate
      update-mouse
      update-mouseover-eid
      check-open-debug
      assoc-active-entities
      set-camera-on-player!
      clear-screen!
      draw-world-map!
      draw-on-world-viewport!
      assoc-interaction-state
      set-cursor!
      player-state-handle-input
      dissoc-interaction-state
      assoc-paused
      update-world-time
      update-potential-fields
      tick-entities
      remove-destroyed-entities
      window-camera-controls
      render-stage
      validate))
