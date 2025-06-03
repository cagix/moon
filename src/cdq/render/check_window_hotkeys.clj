(ns cdq.render.check-window-hotkeys
  (:require [clojure.input :as input]
            [clojure.gdx.ui :as ui]))

(defn- check-escape-close-windows [input windows]
  (when (input/key-just-pressed? input :escape)
    (run! #(ui/set-visible! % false) (ui/children windows))))

(def window-hotkeys {:inventory-window  :i
                     :entity-info-window :e})

(defn- check-window-hotkeys [input windows]
  (doseq [[id input-key] window-hotkeys
          :when (input/key-just-pressed? input input-key)]
    (ui/toggle-visible! (get windows id))))

(defn do! [{:keys [ctx/input
                   ctx/stage]
            :as ctx}]
  (check-window-hotkeys       input (:windows stage))
  (check-escape-close-windows input (:windows stage))
  ctx)
