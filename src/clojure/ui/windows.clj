(ns clojure.ui.windows
  (:require [clojure.ui.entity-info]
            [clojure.ui.inventory]
            [clojure.input :as input]
            [clojure.ui :as ui]))

(defn- check-escape-close-windows [input windows]
  (when (input/key-just-pressed? input :escape)
    (run! #(ui/set-visible! % false) (ui/children windows))))

(def window-hotkeys {:inventory-window  :i
                     :entity-info-window :e})

(defn- check-window-hotkeys [input windows]
  (doseq [[id input-key] window-hotkeys
          :when (input/key-just-pressed? input input-key)]
    (ui/toggle-visible! (get windows id))))

(defn create [{:keys [ctx/ui-viewport]
               :as ctx}]
  (ui/group {:id :windows
             :actors [(ui/actor
                       {:act (fn [this _delta {:keys [ctx/input]}]
                               (check-window-hotkeys       input (ui/parent this))
                               (check-escape-close-windows input (ui/parent this)))})
                      (clojure.ui.entity-info/create [(:width ui-viewport) 0])
                      (clojure.ui.inventory/create ctx
                                               :id :inventory-window
                                               :position [(:width ui-viewport)
                                                          (:height ui-viewport)])]}))
