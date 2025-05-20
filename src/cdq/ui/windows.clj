(ns cdq.ui.windows
  (:require [gdl.input :as input]
            [gdl.ui :as ui]))

(defn- check-escape-close-windows [windows]
  (when (input/key-just-pressed? :escape)
    (run! #(ui/set-visible! % false) (ui/children windows))))

(def window-hotkeys {:inventory-window  :i
                     :entity-info-window :e})

(defn- check-window-hotkeys [windows]
  (doseq [[id input-key] window-hotkeys
          :when (input/key-just-pressed? input-key)]
    (ui/toggle-visible! (get windows id))))

(defn create [& {:keys [id actors]}]
  (ui/group {:id id
             :actors (cons (ui/actor
                            {:act (fn [this _delta]
                                    (check-window-hotkeys       (ui/parent this))
                                    (check-escape-close-windows (ui/parent this)))})
                           actors)}))
