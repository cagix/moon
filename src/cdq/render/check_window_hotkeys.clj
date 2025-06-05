(ns cdq.render.check-window-hotkeys
  (:require [clojure.gdx.ui :as ui]
            [clojure.x :as x]))

(defn- check-escape-close-windows [ctx windows]
  (when (x/key-just-pressed? ctx :escape)
    (run! #(ui/set-visible! % false) (ui/children windows))))

(def window-hotkeys {:inventory-window  :i
                     :entity-info-window :e})

(defn- check-window-hotkeys [ctx windows]
  (doseq [[id input-key] window-hotkeys
          :when (x/key-just-pressed? ctx input-key)]
    (ui/toggle-visible! (get windows id))))

(defn do! [{:keys [ctx/stage]
            :as ctx}]
  (check-window-hotkeys       ctx (:windows stage))
  (check-escape-close-windows ctx (:windows stage))
  ctx)
