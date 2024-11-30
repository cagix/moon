(ns forge.screens.main
  (:require [forge.app :as app]
            [forge.db :as db]
            [forge.graphics :as g]
            [forge.input :refer [key-just-pressed?]]
            [forge.screens.world :as world]
            [forge.ui :as ui]
            [forge.ui.background-image :as background-image]
            [forge.utils :refer [dev-mode?]])
  (:import (com.badlogic.gdx Gdx)))

(defn- exit []
  (.exit Gdx/app))

(defn create []
  {:actors [(background-image/create)
            (ui/table
             {:rows
              (remove nil?
                      (concat
                       (for [world (db/all :properties/worlds)]
                         [(ui/text-button (str "Start " (:property/id world))
                                          #(world/start world))])
                       [(when dev-mode?
                          [(ui/text-button "Map editor"
                                           #(app/change-screen :screens/map-editor))])
                        (when dev-mode?
                          [(ui/text-button "Property editor"
                                           #(app/change-screen :screens/editor))])
                        [(ui/text-button "Exit" exit)]]))
              :cell-defaults {:pad-bottom 25}
              :fill-parent? true})
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :keys/escape)
                                (exit)))})]
   :screen (reify app/Screen
             (enter [_]
               (g/set-cursor :cursors/default))
             (exit [_])
             (render [_])
             (dispose [_]))})
