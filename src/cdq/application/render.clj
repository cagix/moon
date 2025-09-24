; TODO only audio/stage/world/graphics/db...
(ns cdq.application.render
  (:require [cdq.ctx :as ctx]
            [cdq.creature :as creature]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.input :as input]
            cdq.render.draw-on-world-viewport
            cdq.render.tick-entities
            cdq.render.remove-destroyed-entities
            [cdq.stage :as stage]
            [cdq.ui.widget :as widget]
            [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [clojure.graphics.color :as color]
            [clojure.scene2d.stage]
            [clojure.math.vector2 :as v]
            [clojure.utils :as utils]))

(def ^:private pausing? true)

(def ^:private state->pause-game?
  {:stunned false
   :player-moving false
   :player-item-on-cursor true
   :player-idle true
   :player-dead true
   :active-skill false})

(def ^:private zoom-speed 0.025)

(defn- handle-key-input!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/zoom-in?            input) (graphics/change-zoom! graphics zoom-speed))
  (when (input/zoom-out?           input) (graphics/change-zoom! graphics (- zoom-speed)))
  (when (input/close-windows?      input) (stage/close-all-windows!         stage))
  (when (input/toggle-inventory?   input) (stage/toggle-inventory-visible!  stage))
  (when (input/toggle-entity-info? input) (stage/toggle-entity-info-window! stage))
  ctx)

(defn- tick-entities!
  [{:keys [ctx/stage]
    :as ctx}]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (do (try
         (cdq.render.tick-entities/tick-entities! ctx)
         (catch Throwable t
           (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                                 [:tx/show-error-window t]])
           #_(bind-root ::error t)))
        ctx)))

(defn- update-potential-fields!
  [{:keys [ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do
     (world/update-potential-fields! world)
     ctx)))

(defn- update-world-time
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (let [delta-ms (min (graphics/delta-time graphics) (:world/max-delta world))]
      (-> ctx
          (assoc-in [:ctx/world :world/delta-time] delta-ms)
          (update-in [:ctx/world :world/elapsed-time] + delta-ms)))))

(defn- assoc-paused
  [{:keys [ctx/input
           ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (or #_error
                (and pausing?
                     (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                     (not (input/unpause? input))))))

(defn- player-state-handle-input!
  [{:keys [ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        txs (state/handle-input [state-k (state-k entity)]
                                eid
                                ctx)]
    (ctx/handle-txs! ctx txs))
  ctx)

(defn- set-cursor!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        cursor-key (state/cursor [state-k (state-k entity)]
                                 eid
                                 ctx)]
    (graphics/set-cursor! graphics cursor-key))
  ctx)

(defn- player-effect-ctx [mouseover-eid world-mouse-position player-eid]
  (let [target-position (or (and mouseover-eid
                                 (entity/position @mouseover-eid))
                            world-mouse-position)]
    {:effect/source player-eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (entity/position @player-eid) target-position)}))

(defn- interaction-state
  [stage
   world-mouse-position
   mouseover-eid
   player-eid
   mouseover-actor]
  (cond
   mouseover-actor
   [:interaction-state/mouseover-actor (stage/actor-information stage mouseover-actor)]

   (and mouseover-eid
        (:entity/clickable @mouseover-eid))
   [:interaction-state/clickable-mouseover-eid
    {:clicked-eid mouseover-eid
     :in-click-range? (< (entity/distance @player-eid @mouseover-eid)
                         (:entity/click-distance-tiles @player-eid))}]

   :else
   (if-let [skill-id (stage/action-bar-selected-skill stage)]
     (let [entity @player-eid
           skill (skill-id (:entity/skills entity))
           effect-ctx (player-effect-ctx mouseover-eid world-mouse-position player-eid)
           state (creature/skill-usable-state entity skill effect-ctx)]
       (if (= state :usable)
         [:interaction-state.skill/usable [skill effect-ctx]]
         [:interaction-state.skill/not-usable state]))
     [:interaction-state/no-skill-selected])))

(defn- assoc-interaction-state
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (assoc ctx :ctx/interaction-state (interaction-state stage
                                                       (:graphics/world-mouse-position graphics)
                                                       (:world/mouseover-eid world)
                                                       (:world/player-eid    world)
                                                       (stage/mouseover-actor stage (input/mouse-position input)))))

(defn- draw-on-world-viewport!
  [{:keys [ctx/graphics]
    :as ctx}]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (cdq.render.draw-on-world-viewport/do! ctx)))
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

(defn- draw-world-map!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/draw-tiled-map! graphics
                            (:world/tiled-map world)
                            (tile-color-setter
                             {:ray-blocked? (partial world/ray-blocked? world)
                              :explored-tile-corners (:world/explored-tile-corners world)
                              :light-position (graphics/camera-position graphics)
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]}))
  ctx)

(defn- clear-screen!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear! graphics color/black)
  ctx)

(defn- set-camera-on-player!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/set-camera-position! graphics (entity/position @(:world/player-eid world)))
  ctx)

(defn- assoc-active-entities
  [{:keys [ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/active-entities]
            (content-grid/active-entities (:world/content-grid world)
                                          @(:world/player-eid world))))

; TODO also items/skills/mouseover-actors
; -> can separate function get-mouseover-item-for-debug (@ ctx)
(defn- check-open-debug!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/open-debug-button-pressed? input)
    (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                   @((:world/grid world) (mapv int (:graphics/world-mouse-position graphics))))]
      (clojure.scene2d.stage/add! stage (widget/data-viewer
                                         {:title "Data View"
                                          :data data
                                          :width 500
                                          :height 500}))))
  ctx)

(defn- update-mouse
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (let [mouse-position (input/mouse-position input)]
    (update ctx :ctx/graphics #(-> %
                                   (graphics/unproject-ui    mouse-position)
                                   (graphics/unproject-world mouse-position)))))

(defn- update-mouseover-eid!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (let [mouseover-actor (stage/mouseover-actor stage (input/mouse-position input))
        {:keys [world/grid
                world/mouseover-eid
                world/player-eid]} world
        new-eid (if mouseover-actor
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                                     (grid/point->entities grid (:graphics/world-mouse-position graphics)))]
                    (->> (:world/render-z-order world)
                         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
                         reverse
                         (filter #(world/line-of-sight? world player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc-in ctx [:ctx/world :world/mouseover-eid] new-eid)))

(defn- render-stage!
  [{:keys [ctx/stage]
    :as ctx}]
  (clojure.scene2d.stage/set-ctx! stage ctx)
  (clojure.scene2d.stage/act!     stage)
  (clojure.scene2d.stage/draw!    stage)
  (clojure.scene2d.stage/get-ctx  stage))

(defn- try-fetch-state-ctx
  [{:keys [ctx/stage]
    :as ctx}]
  (if-let [new-ctx (clojure.scene2d.stage/get-ctx stage)]
    new-ctx
    ctx)) ; first render stage doesnt have context

(defn do! [context]
  (-> context
      try-fetch-state-ctx
      ctx/validate
      update-mouse
      update-mouseover-eid!
      check-open-debug!
      assoc-active-entities
      set-camera-on-player!
      clear-screen!
      draw-world-map!
      draw-on-world-viewport!
      assoc-interaction-state
      set-cursor!
      player-state-handle-input!
      (dissoc :ctx/interaction-state)
      assoc-paused
      update-world-time
      update-potential-fields!
      tick-entities!
      cdq.render.remove-destroyed-entities/do!
      handle-key-input!
      render-stage!
      ctx/validate))
