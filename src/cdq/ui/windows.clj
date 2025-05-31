(ns cdq.ui.windows
  (:require [cdq.g :as g]
            [gdl.ui :as ui]))

(defn- check-escape-close-windows [ctx windows]
  (when (g/key-just-pressed? ctx :escape)
    (run! #(ui/set-visible! % false) (ui/children windows))))

(def window-hotkeys {:inventory-window  :i
                     :entity-info-window :e})

(defn- check-window-hotkeys [ctx windows]
  (doseq [[id input-key] window-hotkeys
          :when (g/key-just-pressed? ctx input-key)]
    (ui/toggle-visible! (get windows id))))

(defn create [& {:keys [id actors]}]
  (ui/group {:id id
             :actors (cons (ui/actor
                            {:act (fn [this _delta ctx]
                                    (check-window-hotkeys       ctx (ui/parent this))
                                    (check-escape-close-windows ctx (ui/parent this)))})
                           actors)}))
