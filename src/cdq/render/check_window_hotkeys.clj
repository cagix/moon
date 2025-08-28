(ns cdq.render.check-window-hotkeys
  (:require [cdq.input :as input]
            [cdq.ui :as stage]))

(def ^:private close-windows-key  :escape)
(def ^:private toggle-inventory   :i)
(def ^:private toggle-entity-info :e)

(defn do! [{:keys [ctx/input
                   ctx/stage]
            :as ctx}]
  (when (input/key-just-pressed? input close-windows-key)  (stage/close-all-windows!         stage))
  (when (input/key-just-pressed? input toggle-inventory )  (stage/toggle-inventory-visible!  stage))
  (when (input/key-just-pressed? input toggle-entity-info) (stage/toggle-entity-info-window! stage))
  ctx)
