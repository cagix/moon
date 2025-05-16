(ns cdq.ui.windows
  (:require [cdq.ctx :as ctx]
            [cdq.ui.entity-info]
            [cdq.ui.inventory]
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

(defn create []
  (ui/group {:id :windows
             :actors [(proxy [Actor] []
                        (act [_delta]
                          (check-window-hotkeys       (Actor/.getParent this))
                          (check-escape-close-windows (Actor/.getParent this))))
                      (cdq.ui.entity-info/create [(:width ctx/ui-viewport) 0])
                      (cdq.ui.inventory/create [(:width  ctx/ui-viewport)
                                                (:height ctx/ui-viewport)])]}))
