(ns cdq.game.handle-key-input
  (:require [cdq.ctx.input :as input]
            [cdq.ctx.stage :as stage]
            [cdq.gdx.graphics.camera :as camera]))

(def ^:private close-windows-key  :escape)
(def ^:private toggle-inventory   :i)
(def ^:private toggle-entity-info :e)
(def ^:private zoom-speed 0.025)

(defn do!
  [{:keys [ctx/config
           ctx/input
           ctx/graphics
           ctx/stage]
    :as ctx}]
  (let [controls (:controls config)
        camera (:viewport/camera (:world-viewport graphics))]
    (when (input/key-pressed? input (:zoom-in  controls)) (camera/inc-zoom! camera zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (camera/inc-zoom! camera (- zoom-speed)))
    (when (input/key-just-pressed? input close-windows-key)  (stage/close-all-windows!         stage))
    (when (input/key-just-pressed? input toggle-inventory )  (stage/toggle-inventory-visible!  stage))
    (when (input/key-just-pressed? input toggle-entity-info) (stage/toggle-entity-info-window! stage)))
  ctx)
