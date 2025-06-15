(ns cdq.render.check-window-hotkeys
  (:require [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor]))

(defn- check-escape-close-windows [input windows]
  (when (input/key-just-pressed? input :escape)
    (run! #(actor/set-visible! % false) (ui/children windows))))

(def window-hotkeys {:inventory-window  :i
                     :entity-info-window :e})

(defn- check-window-hotkeys [input windows]
  (doseq [[id input-key] window-hotkeys
          :when (input/key-just-pressed? input input-key)]
    (actor/toggle-visible! (get windows id))))

(defn do! [{:keys [ctx/input
                   ctx/stage]
            :as ctx}]
  (check-window-hotkeys       input (:windows stage))
  (check-escape-close-windows input (:windows stage))
  ctx)
