(ns cdq.ui.windows
  (:require [cdq.ctx :as ctx]
            [gdl.input :as input]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn- check-escape-close-windows [windows]
  (when (input/key-just-pressed? :escape)
    (run! #(Actor/.setVisible % false) (ui/children windows))))

(def window-hotkeys {:inventory-window  :i
                     :entity-info-window :e})

(defn- check-window-hotkeys [windows]
  (doseq [[id input-key] window-hotkeys
          :when (input/key-just-pressed? input-key)]
    (ui/toggle-visible! (get windows id))))

(defn create [& {:keys [id actors]}]
  (ui/group {:id id
             :actors (cons (proxy [Actor] []
                             (act [_delta]
                               (check-window-hotkeys       (Actor/.getParent this))
                               (check-escape-close-windows (Actor/.getParent this))))
                           actors)}))
