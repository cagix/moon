(ns cdq.render-pipeline
  (:require [cdq.ctx :as ctx]
            [cdq.gdx.math.vector2 :as v]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.grid :as grid]
            [cdq.malli :as m]
            [cdq.math :as math]
            [cdq.raycaster :as raycaster]
            [cdq.skill :as skill]
            [cdq.stacktrace :as stacktrace]
            [cdq.stage]
            [cdq.tile-color-setter :as tile-color-setter]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.widget :as widget]
            [cdq.utils :as utils]
            [cdq.world :as world]
            [clojure.earlygrey.shape-drawer :as sd]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.graphics.tiled-map-renderer :as tm-renderer]
            [clojure.gdx.graphics.g2d.batch :as batch]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.utils.screen :as screen]
            [clojure.gdx.utils.viewport :as viewport]))

(defn assoc-active-entities
  [ctx]
  (world/assoc-active-entities ctx))

; touch coordinates are y-down, while screen coordinates are y-up
; so the clamping of y is reverse, but as black bars are equal it does not matter
; TODO clamping only works for gui-viewport ?
; TODO ? "Can be negative coordinates, undefined cells."
(defn- unproject-clamp [viewport [x y]]
  (viewport/unproject viewport
                      (math/clamp x
                                  (:viewport/left-gutter-width viewport)
                                  (:viewport/right-gutter-x    viewport))
                      (math/clamp y
                                  (:viewport/top-gutter-height viewport)
                                  (:viewport/top-gutter-y      viewport))))



(defn assoc-mouseover-keys
  [{:keys [ctx/input
           ctx/stage
           ctx/ui-viewport
           ctx/world-viewport]
    :as ctx}]
  (let [mouse-position (input/mouse-position input)
        ui-mouse-position    (unproject-clamp ui-viewport mouse-position)
        world-mouse-position (unproject-clamp world-viewport mouse-position)]
    (assoc ctx
           :ctx/mouseover-actor      (stage/hit stage ui-mouse-position)
           :ctx/ui-mouse-position    ui-mouse-position
           :ctx/world-mouse-position world-mouse-position)))

(def ^:private pausing? true)

(def ^:private state->pause-game?
  {:stunned false
   :player-moving false
   :player-item-on-cursor true
   :player-idle true
   :player-dead true
   :active-skill false})

(defn assoc-paused
  [{:keys [ctx/input
           ctx/player-eid
           ctx/config]
    :as ctx}]
  (assoc ctx :ctx/paused?
         (let [controls (:controls config)]
           (or #_error
               (and pausing?
                    (state->pause-game? (:state (:entity/fsm @player-eid)))
                    (not (or (input/key-just-pressed? input (:unpause-once controls))
                             (input/key-pressed?      input (:unpause-continously controls)))))))))

(defn clear-screen
  [_ctx]
  (screen/clear!))

(defn dissoc-mouseover-keys
  [ctx]
  (dissoc ctx
          :ctx/mouseover-actor
          :ctx/ui-mouse-position
          :ctx/world-mouse-position))

(def ^:private close-windows-key  :escape)
(def ^:private toggle-inventory   :i)
(def ^:private toggle-entity-info :e)
(def ^:private zoom-speed 0.025)

(defn- toggle-entity-info-window! [stage]
  (-> stage :windows :entity-info-window actor/toggle-visible!))

(defn- close-all-windows! [stage]
  (run! #(actor/set-visible! % false) (group/children (:windows stage))))

(defn- action-bar-selected-skill [stage]
  (-> stage
      :action-bar
      action-bar/selected-skill))

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

; TODO try to do this without cond/if !!!
; so have to define a order of handling inputs, etc.
; also in components method no if/else
; => protocol
; => straightforward
; game without ifs
(defn- interaction-state
  [{:keys [ctx/mouseover-actor
           ctx/mouseover-eid
           ctx/stage
           ctx/world-mouse-position
           ctx/player-eid]}]
  (cond
   mouseover-actor
   [:interaction-state/mouseover-actor mouseover-actor]

   (and mouseover-eid
        (:entity/clickable @mouseover-eid))
   [:interaction-state/clickable-mouseover-eid
    {:clicked-eid mouseover-eid
     :in-click-range? (in-click-range? @player-eid @mouseover-eid)}]

   :else
   (if-let [skill-id (action-bar-selected-skill stage)]
     (let [entity @player-eid
           skill (skill-id (:entity/skills entity))
           effect-ctx (player-effect-ctx mouseover-eid world-mouse-position player-eid)
           state (skill/usable-state entity skill effect-ctx)]
       (if (= state :usable)
         [:interaction-state.skill/usable [skill effect-ctx]]
         [:interaction-state.skill/not-usable state]))
     [:interaction-state/no-skill-selected])))

(defn assoc-interaction-state [ctx]
  (assoc ctx :ctx/interaction-state (interaction-state ctx)))

(defn player-state-handle-input
  [{:keys [ctx/player-eid]
    :as ctx}]
  (let [handle-input (state/state->handle-input (:state (:entity/fsm @player-eid)))
        txs (if handle-input
              (handle-input player-eid ctx)
              nil)]
    (ctx/handle-txs! ctx txs))
  nil)

(defn handle-key-input
  [{:keys [ctx/config
           ctx/input
           ctx/stage
           ctx/world-viewport]}]
  (let [controls (:controls config)
        camera (:viewport/camera world-viewport)]
    (when (input/key-pressed? input (:zoom-in  controls)) (camera/inc-zoom! camera zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (camera/inc-zoom! camera (- zoom-speed)))
    (when (input/key-just-pressed? input close-windows-key)  (close-all-windows!         stage))
    (when (input/key-just-pressed? input toggle-inventory )  (cdq.stage/toggle-inventory-visible!  stage))
    (when (input/key-just-pressed? input toggle-entity-info) (toggle-entity-info-window! stage))))

(defn remove-destroyed-entities
  [ctx]
  (world/remove-destroyed-entities! ctx)
  ctx)

(defn render-stage [{:keys [ctx/stage] :as ctx}]
  (stage/set-ctx! stage ctx)
  (stage/act! stage)
  (stage/draw! stage))

(defn set-camera-on-player
  [{:keys [ctx/player-eid
           ctx/world-viewport]}]
  (camera/set-position! (:viewport/camera world-viewport)
                        (:body/position (:entity/body @player-eid))))

(defn set-cursor
  [{:keys [ctx/cursors
           ctx/graphics
           ctx/player-eid]
    :as ctx}]
  (let [cursor-key (let [->cursor (state/state->cursor (:state (:entity/fsm @player-eid)))]
                     (if (keyword? ->cursor)
                       ->cursor
                       (->cursor player-eid ctx)))]
    (assert (contains? cursors cursor-key))
    (graphics/set-cursor! graphics (get cursors cursor-key))))

(defn- update-time
  [{:keys [ctx/graphics
           ctx/max-delta]
    :as ctx}]
  (let [delta-ms (min (graphics/delta-time graphics) max-delta)]
    (-> ctx
        (assoc :ctx/delta-time delta-ms)
        (update :ctx/elapsed-time + delta-ms))))

(defn update-potential-fields!
  [ctx]
  (world/update-potential-fields! ctx)
  ctx)

(defn- tick-entities!
  [{:keys [ctx/stage]
    :as ctx}]
  (try
   (world/tick-entities! ctx)
   (catch Throwable t
     (stacktrace/pretty-print t)
     (stage/add! stage (widget/error-window t))
     #_(bind-root ::error t)))
  ctx)

(defn tick-world
  [ctx]
  (if (:ctx/paused? ctx)
    ctx
    (-> ctx
        update-time
        update-potential-fields!
        tick-entities!)))

(defn- validate [{:keys [ctx/schema] :as ctx}]
  (m/validate-humanize schema ctx))

(defn- update-mouseover-eid
  [{:keys [ctx/mouseover-actor
           ctx/mouseover-eid
           ctx/player-eid
           ctx/render-z-order
           ctx/raycaster
           ctx/grid
           ctx/world-mouse-position]
    :as ctx}]
  (let [new-eid (if mouseover-actor
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                                     (grid/point->entities grid world-mouse-position))]
                    (->> render-z-order
                         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
                         reverse
                         (filter #(raycaster/line-of-sight? raycaster player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))

(defn draw-world-map
  [{:keys [ctx/explored-tile-corners
           ctx/tiled-map
           ctx/tiled-map-renderer
           ctx/raycaster
           ctx/world-viewport]}]
  (tm-renderer/draw! tiled-map-renderer
                     world-viewport
                     tiled-map
                     (tile-color-setter/create
                      {:ray-blocked? (partial raycaster/blocked? raycaster)
                       :explored-tile-corners explored-tile-corners
                       :light-position (:camera/position (:viewport/camera world-viewport))
                       :see-all-tiles? false
                       :explored-tile-color  [0.5 0.5 0.5 1]
                       :visible-tile-color   [1 1 1 1]
                       :invisible-tile-color [0 0 0 1]})))

(defn draw-on-world-viewport
  [{:keys [ctx/config
           ctx/batch
           ctx/shape-drawer
           ctx/unit-scale
           ctx/world-unit-scale
           ctx/world-viewport]
    :as ctx}]
  ; fix scene2d.ui.tooltip flickering ( maybe because I dont call super at act Actor which is required ...)
  ; -> also Widgets, etc. ? check.
  (batch/set-color! batch color/white)
  (batch/set-projection-matrix! batch (:camera/combined (:viewport/camera world-viewport)))
  (batch/begin! batch)
  (sd/with-line-width shape-drawer world-unit-scale
    (fn []
      (reset! unit-scale world-unit-scale)
      (doseq [f (:draw-on-world-viewport (:cdq.render-pipeline config))]
        ((requiring-resolve f) ctx))
      (reset! unit-scale 1)))
  (batch/end! batch))

; TODO also items/skills/mouseover-actors
; -> can separate function get-mouseover-item-for-debug (@ ctx)

(defn- open-debug-data-window!
  [{:keys [ctx/stage
           ctx/mouseover-eid
           ctx/grid
           ctx/world-mouse-position]}]
  (let [data (or (and mouseover-eid @mouseover-eid)
                 @(grid/cell grid
                             (mapv int world-mouse-position)))]
    (stage/add! stage (widget/data-viewer
                       {:title "Data View"
                        :data data
                        :width 500
                        :height 500}))))

(defn check-open-debug-data
  [{:keys [ctx/input] :as ctx}]
  (when (input/button-just-pressed? input :right)
    (open-debug-data-window! ctx)))

(defn do!
  [ctx]
  (reduce (fn [ctx f]
            (if-let [new-ctx (f ctx)]
              new-ctx
              ctx))
          ctx
          [validate
           assoc-mouseover-keys
           update-mouseover-eid
           check-open-debug-data
           assoc-active-entities
           set-camera-on-player
           clear-screen
           draw-world-map
           draw-on-world-viewport
           render-stage
           assoc-interaction-state
           set-cursor
           player-state-handle-input
           assoc-paused
           tick-world
           remove-destroyed-entities ; do not pause as pickup item should be destroyed
           handle-key-input
           dissoc-mouseover-keys
           validate]))
