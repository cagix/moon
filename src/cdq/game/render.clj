(ns cdq.game.render
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.graphics :as graphics]
            [cdq.ctx.input :as input]
            [cdq.ctx.stage :as stage]
            [cdq.ctx.world :as world]
            [cdq.gdx.graphics.camera :as camera]
            [cdq.stacktrace :as stacktrace]
            [cdq.world.entity :as entity]
            [cdq.ui.stage]
            [cdq.ui.error-window :as error-window]))

(declare entity->tick)

(defn render-stage! [{:keys [ctx/stage] :as ctx}]
  (cdq.ui.stage/render! stage ctx))

(declare state->cursor)

(defn set-cursor!
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

(defn player-state-handle-input!
  [{:keys [ctx/player-eid]
    :as ctx}]
  (let [handle-input (state->handle-input (:state (:entity/fsm @player-eid)))
        txs (if handle-input
              (handle-input player-eid ctx)
              nil)]
    (ctx/handle-txs! ctx txs))
  ctx)

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

(defn tick-world!
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

(defn check-window-hotkeys!
  [{:keys [ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/key-just-pressed? input close-windows-key)  (stage/close-all-windows!         stage))
  (when (input/key-just-pressed? input toggle-inventory )  (stage/toggle-inventory-visible!  stage))
  (when (input/key-just-pressed? input toggle-entity-info) (stage/toggle-entity-info-window! stage))
  ctx)

(defn check-camera-controls!
  [{:keys [ctx/config
           ctx/input
           ctx/graphics]
    :as ctx}]
  (let [controls (:controls config)
        camera (:viewport/camera (:world-viewport graphics))]
    (when (input/key-pressed? input (:zoom-in  controls)) (camera/inc-zoom! camera zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (camera/inc-zoom! camera (- zoom-speed))))
  ctx)
