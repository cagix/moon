(ns cdq.game.render
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.graphics :as graphics]
            [cdq.ctx.input :as input]
            [cdq.ctx.stage :as stage]
            [cdq.ctx.world :as world]
            [cdq.dev.data-view :as data-view]
            [cdq.gdx.graphics.camera :as camera]
            [cdq.gdx.math.geom :as geom]
            [cdq.raycaster :as raycaster]
            [cdq.stacktrace :as stacktrace]
            [cdq.tile-color-setter :as tile-color-setter]
            [cdq.world.entity :as entity]
            [cdq.world.grid :as grid]
            [cdq.ui.stage]
            [cdq.ui.error-window :as error-window]
            [cdq.utils :as utils]))

(defn- check-open-debug-data-view!
  [{:keys [ctx/input
           ctx/stage
           ctx/mouseover-eid
           ctx/world
           ctx/world-mouse-position]
    :as ctx}]
  (when (input/button-just-pressed? input :right)
    (let [data (or (and mouseover-eid @mouseover-eid)
                   @(grid/cell (:world/grid world)
                               (mapv int world-mouse-position)))]
      (cdq.ui.stage/add! stage
                         (data-view/table-view-window {:title "Data View"
                                                       :data data
                                                       :width 500
                                                       :height 500}))))
  ctx)

(defn- assoc-active-entities [ctx]
  (update ctx :ctx/world world/cache-active-entities @(:ctx/player-eid ctx)))

(defn- set-camera-on-player!
  [{:keys [ctx/graphics
           ctx/player-eid]
    :as ctx}]
  (camera/set-position! (:viewport/camera (:world-viewport graphics))
                        (entity/position @player-eid))
  ctx)

(defn- clear-screen!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear-screen! graphics :black)
  ctx)

(defn- draw-world-map!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/draw-tiled-map! graphics
                            (:world/tiled-map world)
                            (tile-color-setter/create
                             {:ray-blocked? (let [raycaster (:world/raycaster world)]
                                              (fn [start end] (raycaster/blocked? raycaster start end)))
                              :explored-tile-corners (:world/explored-tile-corners world)
                              :light-position (:camera/position (:viewport/camera (:world-viewport graphics)))
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]}))
  ctx)

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

(declare entity->tick
         render-layers)

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
        player @player-eid
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

(defn- draw-on-world-viewport!
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

(defn- render-stage! [{:keys [ctx/stage] :as ctx}]
  (cdq.ui.stage/render! stage ctx))

(declare state->cursor)

(defn- set-cursor!
  [{:keys [ctx/graphics
           ctx/player-eid]
    :as ctx}]
  ; world/player-state
  (graphics/set-cursor! graphics (let [->cursor (state->cursor (:state (:entity/fsm @player-eid)))]
                                   (if (keyword? ->cursor)
                                     ->cursor
                                     (->cursor player-eid ctx))))
  ctx)

(def state->handle-input)

(defn- player-state-handle-input!
  [{:keys [ctx/player-eid]
    :as ctx}]
  (let [handle-input (state->handle-input (:state (:entity/fsm @player-eid)))
        txs (if handle-input
              (handle-input player-eid ctx)
              nil)]
    (ctx/handle-txs! ctx txs))
  ctx)

(defn- update-mouseover-entity!
  [{:keys [ctx/mouseover-actor
           ctx/mouseover-eid
           ctx/player-eid
           ctx/world
           ctx/world-mouse-position]
    :as ctx}]
  (let [new-eid (if mouseover-actor
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                                     (grid/point->entities (:world/grid world) world-mouse-position))]
                    (->> (:world/render-z-order world)
                         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
                         reverse
                         (filter #(world/line-of-sight? world player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))

(def ^:private pausing? true)

(def ^:private state->pause-game?
  {:stunned false
   :player-moving false
   :player-item-on-cursor true
   :player-idle true
   :player-dead true
   :active-skill false})

(defn- assoc-paused
  [{:keys [ctx/input
           ctx/player-eid
           ctx/config]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (let [controls (:controls config)]
              (or #_error
                  (and pausing?
                       (state->pause-game? (:state (:entity/fsm @player-eid)))
                       (not (or (input/key-just-pressed? input (:unpause-once controls))
                                (input/key-pressed?      input (:unpause-continously controls)))))))))

(defn- update-time [{:keys [ctx/graphics
                            ctx/world]
                     :as ctx}]
  (update ctx :ctx/world world/update-time (graphics/delta-time graphics)))

(defn- update-potential-fields!
  [{:keys [ctx/world]
    :as ctx}]
  (world/tick-potential-fields! world)
  ctx)

; (defmulti tick! (fn [[k] _v _eid _world]
;                   k))
; (defmethod tick! :default [_ _v _eid _world])

(defn- tick-component! [k v eid world]
  (when-let [f (entity->tick k)]
    (f v eid world)))

(defn- tick-entity! [{:keys [ctx/world] :as ctx} eid]
  (doseq [k (keys @eid)]
    (try (when-let [v (k @eid)]
           (ctx/handle-txs! ctx (tick-component! k v eid world)))
         (catch Throwable t
           (throw (ex-info "entity-tick"
                           {:k k
                            :entity/id (entity/id @eid)}
                           t))))))
(defn- tick-entities!
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (try
   (doseq [eid (:world/active-entities world)]
     (tick-entity! ctx eid))
   (catch Throwable t
     (stacktrace/pretty-print t)
     (cdq.ui.stage/add! stage (error-window/create t))
     #_(bind-root ::error t)))
  ctx)

(defn- tick-world!
  [ctx]
  (if (get-in ctx [:ctx/world :world/paused?])
    ctx
    (-> ctx
        update-time
        update-potential-fields!
        tick-entities!)))

(def ^:private close-windows-key  :escape)
(def ^:private toggle-inventory   :i)
(def ^:private toggle-entity-info :e)
(def ^:private zoom-speed 0.025)

(defn- assoc-mouseover-keys
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (let [mouse-position (input/mouse-position input)
        ui-mouse-position    (graphics/unproject-ui    graphics mouse-position)
        world-mouse-position (graphics/unproject-world graphics mouse-position)]
    (assoc ctx
           :ctx/mouseover-actor      (stage/hit stage ui-mouse-position)
           :ctx/ui-mouse-position    ui-mouse-position
           :ctx/world-mouse-position world-mouse-position)))

(defn- dissoc-mouseover-keys
  [ctx]
  (dissoc ctx
          :ctx/mouseover-actor
          :ctx/ui-mouse-position
          :ctx/world-mouse-position))

(defn- check-window-hotkeys!
  [{:keys [ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/key-just-pressed? input close-windows-key)  (stage/close-all-windows!         stage))
  (when (input/key-just-pressed? input toggle-inventory )  (stage/toggle-inventory-visible!  stage))
  (when (input/key-just-pressed? input toggle-entity-info) (stage/toggle-entity-info-window! stage))
  ctx)

(defn- check-camera-controls!
  [{:keys [ctx/config
           ctx/input
           ctx/graphics]
    :as ctx}]
  (let [controls (:controls config)]
    (when (input/key-pressed? input (:zoom-in  controls)) (graphics/zoom-in!  graphics zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (graphics/zoom-out! graphics zoom-speed)))
  ctx)

(defn- remove-destroyed-entities!
  [{:keys [ctx/world]
    :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @(:world/entity-ids world)))]
    (ctx/handle-txs! ctx (world/remove-entity! world eid)))
  ctx)

(defn do! [ctx]
  (-> ctx
      ;validate
      assoc-mouseover-keys
      update-mouseover-entity!
      check-open-debug-data-view! ; TODO FIXME its not documented I forgot rightclick can open debug data view!
      assoc-active-entities
      set-camera-on-player!
      clear-screen!
      draw-world-map!
      draw-on-world-viewport!
      render-stage!
      set-cursor!
      player-state-handle-input!
      assoc-paused
      tick-world!
      remove-destroyed-entities! ; do not pause as pickup item should be destroyed
      check-camera-controls!
      check-window-hotkeys!
      dissoc-mouseover-keys
      ;validate

      ))
