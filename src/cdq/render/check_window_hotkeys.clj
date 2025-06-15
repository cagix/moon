(ns cdq.render.check-window-hotkeys
  (:require [gdl.input :as input]
            [gdl.ui.actor :as actor]
            [gdl.ui.group :as group]))

(defn- check-escape-close-windows [input windows]
  (when (input/key-just-pressed? input :escape)
    (run! #(actor/set-visible! % false) (group/children windows))))

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
