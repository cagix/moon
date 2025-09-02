(ns cdq.game.render
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.graphics :as graphics]
            [cdq.ctx.input :as input]
            [cdq.ctx.stage :as stage]
            [cdq.gdx.graphics.camera :as camera]
            [cdq.ui.stage]))

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
