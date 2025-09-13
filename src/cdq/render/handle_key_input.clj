(ns cdq.render.handle-key-input
  (:require [cdq.ctx.stage]
            [cdq.ctx.graphics :as graphics]
            [clojure.gdx.input :as input]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]))

(def ^:private close-windows-key  :escape)
(def ^:private toggle-inventory   :i)
(def ^:private toggle-entity-info :e)
(def ^:private zoom-speed 0.025)

(defn- toggle-entity-info-window! [stage]
  (-> stage :windows :entity-info-window actor/toggle-visible!))

(defn- close-all-windows! [stage]
  (run! #(actor/set-visible! % false) (group/children (:windows stage))))

(defn do!
  [{:keys [ctx/config
           ctx/graphics
           ctx/input
           ctx/stage]}]
  (let [controls (:controls config)]
    (when (input/key-pressed? input (:zoom-in  controls)) (graphics/change-zoom! graphics zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (graphics/change-zoom! graphics (- zoom-speed)))
    (when (input/key-just-pressed? input close-windows-key)  (close-all-windows!         stage))
    (when (input/key-just-pressed? input toggle-inventory )  (cdq.ctx.stage/toggle-inventory-visible!  stage))
    (when (input/key-just-pressed? input toggle-entity-info) (toggle-entity-info-window! stage))))
