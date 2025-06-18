(ns cdq.render.check-window-hotkeys
  (:require [gdl.input :as input]
            [cdq.ui :as stage]))

(defn do! [{:keys [ctx/input
                   ctx/stage]
            :as ctx}
           {:keys [close-windows-key
                   toggle-inventory
                   toggle-entity-info]}]
  (when (input/key-just-pressed? input close-windows-key)  (stage/close-all-windows!         stage))
  (when (input/key-just-pressed? input toggle-inventory )  (stage/toggle-inventory-visible!  stage))
  (when (input/key-just-pressed? input toggle-entity-info) (stage/toggle-entity-info-window! stage))
  ctx)
