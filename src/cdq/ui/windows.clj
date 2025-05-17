(ns cdq.ui.windows
  (:require [cdq.ctx :as ctx]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

(defn- check-escape-close-windows [windows]
  (when (input/key-just-pressed? :escape)
    (run! #(Actor/.setVisible % false) (Group/.getChildren windows))))

(def window-hotkeys {:inventory-window  :i
                     :entity-info-window :e})

(defn- check-window-hotkeys [windows]
  (doseq [[id input-key] window-hotkeys
          :when (input/key-just-pressed? input-key)]
    (actor/toggle-visible! (get windows id))))

(defn create [& {:keys [id actors]}]
  (ui/group {:id id
             :actors (cons (proxy [Actor] []
                             (act [_delta]
                               (check-window-hotkeys       (Actor/.getParent this))
                               (check-escape-close-windows (Actor/.getParent this))))
                           actors)}))
