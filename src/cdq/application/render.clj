(ns cdq.application.render
  (:require [cdq.ctx :as ctx]
            [cdq.creature :as creature]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.stage :as stage]
            [cdq.ui.widget :as widget]
            [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [clojure.graphics.color :as color]
            [clojure.input :as input]
            [clojure.scene2d.stage]
            [clojure.math.vector2 :as v]
            [clojure.utils :as utils]
            cdq.render.draw-on-world-viewport.tile-grid
            cdq.render.draw-on-world-viewport.cell-debug
            cdq.render.draw-on-world-viewport.entities
          #_cdq.render.draw-on-world-viewport.geom-test
            cdq.render.draw-on-world-viewport.highlight-mouseover-tile
            cdq.render.tick-entities
            cdq.render.remove-destroyed-entities
            [clojure.utils :as utils]))

(def ^:private pausing? true)

(def ^:private state->pause-game?
  {:stunned false
   :player-moving false
   :player-item-on-cursor true
   :player-idle true
   :player-dead true
   :active-skill false})

(def ^:private close-windows-key  :escape)
(def ^:private toggle-inventory   :i)
(def ^:private toggle-entity-info :e)
(def ^:private zoom-speed 0.025)

(defn- handle-key-input!
  [{:keys [ctx/controls
           ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/key-pressed? input (:zoom-in  controls)) (graphics/change-zoom! graphics zoom-speed))
  (when (input/key-pressed? input (:zoom-out controls)) (graphics/change-zoom! graphics (- zoom-speed)))
  (when (input/key-just-pressed? input close-windows-key)  (stage/close-all-windows!         stage))
  (when (input/key-just-pressed? input toggle-inventory )  (stage/toggle-inventory-visible!  stage))
  (when (input/key-just-pressed? input toggle-entity-info) (stage/toggle-entity-info-window! stage))
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
  [ctx]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (do
     (ctx/handle-txs! ctx [[:tx/update-potential-fields]])
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
           ctx/controls
           ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (or #_error
                (and pausing?
                     (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                     (not (or (input/key-just-pressed? input (:unpause-once controls))
                              (input/key-pressed?      input (:unpause-continously controls))))))))

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

(defn- distance [a b]
  (v/distance (entity/position a)
              (entity/position b)))

(defn- in-click-range? [player-entity clicked-entity]
  (< (distance player-entity clicked-entity)
     (:entity/click-distance-tiles player-entity)))

(defn- player-effect-ctx [mouseover-eid world-mouse-position player-eid]
  (let [target-position (or (and mouseover-eid
                                 (entity/position @mouseover-eid))
                            world-mouse-position)]
    {:effect/source player-eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (entity/position @player-eid) target-position)}))

(defn- interaction-state
  [{:keys [ctx/mouseover-actor
           ctx/stage
           ctx/world-mouse-position]}
   mouseover-eid
   player-eid]
  (cond
   mouseover-actor
   [:interaction-state/mouseover-actor
    (stage/actor-information stage mouseover-actor)]

   (and mouseover-eid
        (:entity/clickable @mouseover-eid))
   [:interaction-state/clickable-mouseover-eid
    {:clicked-eid mouseover-eid
     :in-click-range? (in-click-range? @player-eid @mouseover-eid)}]

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

(defn- assoc-interaction-state [ctx]
  (assoc ctx :ctx/interaction-state (interaction-state ctx
                                                       (:world/mouseover-eid (:ctx/world ctx))
                                                       (:world/player-eid (:ctx/world ctx)))))

(defn- draw-on-world-viewport!
  [{:keys [ctx/graphics]
    :as ctx}
   draw-fns]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (doseq [[f & params] draw-fns]
                                        (graphics/handle-draws! graphics (apply f ctx params)))))
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
  (graphics/set-camera-position! graphics
                                 (:body/position (:entity/body @(:world/player-eid world))))
  ctx)

(defn- assoc-active-entities
  [{:keys [ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/active-entities]
            (content-grid/active-entities (:world/content-grid world)
                                          @(:world/player-eid world))))

; TODO also items/skills/mouseover-actors
; -> can separate function get-mouseover-item-for-debug (@ ctx)
(defn- open-debug-data-window!
  [{:keys [ctx/stage
           ctx/world
           ctx/world-mouse-position]}]
  (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                 @((:world/grid world) (mapv int world-mouse-position)))]
    (clojure.scene2d.stage/add! stage (widget/data-viewer
                                       {:title "Data View"
                                        :data data
                                        :width 500
                                        :height 500}))))

(defn- check-open-debug!
  [{:keys [ctx/input] :as ctx}]
  (when (input/button-just-pressed? input :right)
    (open-debug-data-window! ctx))
  ctx)

(defn- update-mouse
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (let [mouse-position (input/mouse-position input)
        ui-mouse-position    (graphics/unproject-ui    graphics mouse-position)
        world-mouse-position (graphics/unproject-world graphics mouse-position)]
    (assoc ctx
           :ctx/mouseover-actor      (clojure.scene2d.stage/hit stage ui-mouse-position)
           :ctx/ui-mouse-position    ui-mouse-position
           :ctx/world-mouse-position world-mouse-position)))

(defn- update-mouseover-eid!
  [{:keys [ctx/mouseover-actor
           ctx/world
           ctx/world-mouse-position]
    :as ctx}]
  (let [{:keys [world/grid
                world/mouseover-eid
                world/player-eid]} world
        new-eid (if mouseover-actor
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                                     (grid/point->entities grid world-mouse-position))]
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

(defn- try-fetch-state-ctx [ctx]
  (if-let [new-ctx (clojure.scene2d.stage/get-ctx (:ctx/stage ctx))]
    new-ctx
    ctx ; first render stage doesnt have context
    ))

(def ^:private pipeline
  [[try-fetch-state-ctx]
   [ctx/validate]
   [update-mouse]
   [update-mouseover-eid!]
   [check-open-debug!]
   [assoc-active-entities]
   [set-camera-on-player!]
   [clear-screen!]
   [draw-world-map!]
   [draw-on-world-viewport! [
                             [cdq.render.draw-on-world-viewport.tile-grid/do!]
                             [cdq.render.draw-on-world-viewport.cell-debug/do!]
                             [cdq.render.draw-on-world-viewport.entities/do!]
                             #_ [cdq.render.draw-on-world-viewport.geom-test/do!]
                             [cdq.render.draw-on-world-viewport.highlight-mouseover-tile/do!]
                             ]]
   [assoc-interaction-state]
   [set-cursor!]
   [player-state-handle-input!]
   [assoc-paused]
   [update-world-time]
   [update-potential-fields!]
   [tick-entities!]
   [cdq.render.remove-destroyed-entities/do!]
   [handle-key-input!]
   [render-stage!]
   [ctx/validate]])

(defn do! [context]
  (utils/pipeline context pipeline))
