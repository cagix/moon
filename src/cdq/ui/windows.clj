(ns cdq.ui.windows
  (:require [clojure.gdx.input :as input]
            [gdl.ui :as ui]))

(defn- check-escape-close-windows [input windows]
  (when (input/key-just-pressed? input :escape)
    (run! #(ui/set-visible! % false) (ui/children windows))))

(def window-hotkeys {:inventory-window  :i
                     :entity-info-window :e})

(defn- check-window-hotkeys [input windows]
  (doseq [[id input-key] window-hotkeys
          :when (input/key-just-pressed? input input-key)]
    (ui/toggle-visible! (get windows id))))

(defn create [& {:keys [id actors]}]
  (ui/group {:id id
             :actors (cons (ui/actor
                            {:act (fn [this _delta {:keys [ctx/input]}]
                                    (check-window-hotkeys       input (ui/parent this))
                                    (check-escape-close-windows input (ui/parent this)))})
                           actors)}))
