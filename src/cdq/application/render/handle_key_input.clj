(ns cdq.application.render.handle-key-input
  (:require [cdq.graphics :as graphics]
            [cdq.stage :as stage]
            [gdl.input :as input]))

(def ^:private close-windows-key  :escape)
(def ^:private toggle-inventory   :i)
(def ^:private toggle-entity-info :e)
(def ^:private zoom-speed 0.025)

(defn do!
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
