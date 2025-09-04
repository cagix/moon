(ns cdq.render.handle-key-input
  (:require [cdq.input :as input]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.gdx.graphics.camera :as camera]))

(def ^:private close-windows-key  :escape)
(def ^:private toggle-inventory   :i)
(def ^:private toggle-entity-info :e)
(def ^:private zoom-speed 0.025)

(defn toggle-inventory-visible! [stage]
  (-> stage :windows :inventory-window actor/toggle-visible!))

(defn- toggle-entity-info-window! [stage]
  (-> stage :windows :entity-info-window actor/toggle-visible!))

(defn- close-all-windows! [stage]
  (run! #(actor/set-visible! % false) (group/children (:windows stage))))

(defn do!
  [{:keys [ctx/config
           ctx/input
           ctx/stage
           ctx/world-viewport]}]
  (let [controls (:controls config)
        camera (:viewport/camera world-viewport)]
    (when (input/key-pressed? input (:zoom-in  controls)) (camera/inc-zoom! camera zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (camera/inc-zoom! camera (- zoom-speed)))
    (when (input/key-just-pressed? input close-windows-key)  (close-all-windows!         stage))
    (when (input/key-just-pressed? input toggle-inventory )  (toggle-inventory-visible!  stage))
    (when (input/key-just-pressed? input toggle-entity-info) (toggle-entity-info-window! stage))))
